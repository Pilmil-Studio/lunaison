#!/usr/bin/env python3
"""Mesure la couverture des traductions de Lunaison, langue par langue.

Lunaison écrit ses textes côte à côte avec `tr(fr, en, pl = ..., uk = ..., es = ..., de = ...)`
(voir core/model/Language.kt). Une langue absente d'un appel retombe sur l'anglais : le texte
s'affiche, mais pas dans la langue du téléphone. Ce script compte, pour chaque langue, les textes
réellement traduits :

- appels `tr(...)` et `tr6(...)` du code (hors tests) ;
- fiches de la rubrique Comprendre (Articles.kt et ArticlesXx.kt) ;
- chaînes Android (`app/src/main/res/values-xx/strings.xml`).

Il signale aussi les textes affichés en dur, hors `tr`, qui ne sont jamais traduits.

Usage :
    python3 scripts/couverture-langues.py            # affiche le tableau
    python3 scripts/couverture-langues.py --readme   # met à jour la section du README
    python3 scripts/couverture-langues.py --details  # liste les textes non traduits
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANGS = ["fr", "en", "pl", "uk", "es", "de"]
NAMES = {"fr": "Français", "en": "Anglais", "pl": "Polonais", "uk": "Ukrainien", "es": "Espagnol", "de": "Allemand"}
OPTIONAL = ["pl", "uk", "es", "de"]
BEGIN = "<!-- couverture-langues:debut -->"
END = "<!-- couverture-langues:fin -->"


def kotlin_sources() -> list[Path]:
    files = []
    for path in ROOT.rglob("*.kt"):
        rel = path.relative_to(ROOT).as_posix()
        if "/build/" in rel or "Test" in rel or "/test/" in rel or rel.startswith("screenshots/"):
            continue
        files.append(path)
    return sorted(files)


def skip_string(src: str, i: int) -> int:
    """Renvoie l'index juste après la chaîne Kotlin qui commence en i."""
    if src.startswith('"""', i):
        end = src.find('"""', i + 3)
        return len(src) if end < 0 else end + 3
    j = i + 1
    depth = 0  # gabarits ${...}
    while j < len(src):
        c = src[j]
        if c == "\\":
            j += 2
            continue
        if c == "$" and j + 1 < len(src) and src[j + 1] == "{":
            depth += 1
            j += 2
            continue
        if c == "}" and depth:
            depth -= 1
        elif c == '"' and not depth:
            return j + 1
        j += 1
    return j


def call_args(src: str, open_paren: int) -> tuple[list[str], int]:
    """Découpe les arguments d'un appel à partir de sa parenthèse ouvrante."""
    args, start, depth, i = [], open_paren + 1, 0, open_paren + 1
    while i < len(src):
        c = src[i]
        if c == '"':
            i = skip_string(src, i)
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            if depth == 0:
                args.append(src[start:i].strip())
                return [a for a in args if a], i + 1
            depth -= 1
        elif c == "," and depth == 0:
            args.append(src[start:i].strip())
            start = i + 1
        i += 1
    return args, i


CALL = re.compile(r"(?<![\w.])(tr6?)\s*\(")


def scan_tr(files: list[Path]):
    calls = []  # (fichier, ligne, langues présentes, texte fr)
    for path in files:
        src = path.read_text(encoding="utf-8")
        if path.name == "Language.kt":
            continue
        for m in CALL.finditer(src):
            # Ignore les déclarations (`fun tr(`).
            before = src[max(0, m.start() - 4):m.start()]
            if before.endswith("fun "):
                continue
            args, _ = call_args(src, m.end() - 1)
            # Une adresse (site en français ou en anglais) n'est pas un texte à traduire.
            if len(args) < 2 or args[0].startswith('"http'):
                continue
            present = {"fr", "en"}
            if m.group(1) == "tr6":
                present = set(LANGS) if len(args) >= 6 else present
            else:
                positional = [a for a in args if not re.match(r"^\w+\s*=", a)]
                named = {a.split("=", 1)[0].strip() for a in args if re.match(r"^\w+\s*=", a)}
                for k, lang in enumerate(["fr", "en", "pl", "uk", "es", "de"][: len(positional)]):
                    present.add(lang)
                present |= {lang for lang in named if lang in LANGS}
            line = src.count("\n", 0, m.start()) + 1
            calls.append((path.relative_to(ROOT).as_posix(), line, present, args[0][:70]))
    return calls


def count_articles() -> dict[str, int]:
    content = ROOT / "core/content/src/commonMain/kotlin/fr/lunaison/core/content"
    counts = {}
    for lang, name in [("fr", "Articles.kt"), ("en", "ArticlesEn.kt"), ("pl", "ArticlesPl.kt"),
                       ("uk", "ArticlesUk.kt"), ("es", "ArticlesEs.kt"), ("de", "ArticlesDe.kt")]:
        path = content / name
        counts[lang] = len(re.findall(r"=\s*Article\(", path.read_text(encoding="utf-8"))) if path.exists() else 0
    return counts


def count_android_strings() -> dict[str, int]:
    res = ROOT / "app/src/main/res"
    counts = {}
    for lang in LANGS:
        folder = "values" if lang == "en" else f"values-{lang}"
        path = res / folder / "strings.xml"
        counts[lang] = len(re.findall(r"<string\s+name=", path.read_text(encoding="utf-8"))) if path.exists() else 0
    return counts


HARDCODED = re.compile(r"\bText\(\s*\"([^\"$]*[A-Za-zÀ-ÿ]{3,}[^\"$]*)\"")


def scan_hardcoded(files: list[Path]):
    found = []
    for path in files:
        rel = path.relative_to(ROOT).as_posix()
        if not rel.startswith(("ui/", "app/")):
            continue
        for n, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            for m in HARDCODED.finditer(line):
                text = m.group(1)
                if re.fullmatch(r"[\w.%\- ]*Lunaison[\w ]*|[A-Z0-9 .:/-]+", text):
                    continue
                found.append((rel, n, text[:60]))
    return found


def pct(n: int, total: int) -> str:
    return "100 %" if total == 0 or n == total else f"{100 * n / total:.0f} %"


def table(calls, articles, strings, hardcoded) -> str:
    total = len(calls)
    rows = [
        "| Langue | Textes de l'appli (`tr`) | Fiches Comprendre | Chaînes Android | Couverture |",
        "|---|---|---|---|---|",
    ]
    for lang in LANGS:
        done = sum(1 for c in calls if lang in c[2])
        art = f"{articles[lang]} / {articles['fr']}"
        st = f"{strings[lang]} / {strings['en']}"
        parts = [done / total if total else 1, articles[lang] / articles["fr"] if articles["fr"] else 1,
                 strings[lang] / strings["en"] if strings["en"] else 1]
        weights = [total, articles["fr"], strings["en"]]
        overall = sum(p * w for p, w in zip(parts, weights)) / sum(weights)
        rows.append(f"| {NAMES[lang]} | {done} / {total} ({pct(done, total)}) | {art} | {st} | **{overall * 100:.0f} %** |")
    note = (
        f"\n{len(hardcoded)} texte(s) affiché(s) en dur hors `tr`, donc jamais traduit(s)."
        if hardcoded else "\nAucun texte affiché en dur hors `tr` repéré."
    )
    return "\n".join(rows) + "\n" + note + (
        "\nUn texte non traduit s'affiche en anglais. Tableau produit par `scripts/couverture-langues.py`."
    )


def main() -> int:
    files = kotlin_sources()
    calls = scan_tr(files)
    articles = count_articles()
    strings = count_android_strings()
    hardcoded = scan_hardcoded(files)
    result = table(calls, articles, strings, hardcoded)

    if "--details" in sys.argv:
        for lang in OPTIONAL:
            missing = [c for c in calls if lang not in c[2]]
            print(f"\n## {NAMES[lang]} : {len(missing)} texte(s) non traduit(s)")
            for path, line, _, text in missing:
                print(f"- {path}:{line}  {text}")
        print("\n## Textes en dur")
        for path, line, text in hardcoded:
            print(f"- {path}:{line}  {text}")
        return 0

    if "--readme" in sys.argv:
        readme = ROOT / "README.md"
        content = readme.read_text(encoding="utf-8")
        block = f"{BEGIN}\n{result}\n{END}"
        if BEGIN in content:
            content = re.sub(re.escape(BEGIN) + r".*?" + re.escape(END), lambda _: block, content, flags=re.S)
        else:
            print("Marqueurs absents du README : ajoutez la section avant de relancer.", file=sys.stderr)
            return 1
        readme.write_text(content, encoding="utf-8")
    print(result)
    return 0


if __name__ == "__main__":
    sys.exit(main())
