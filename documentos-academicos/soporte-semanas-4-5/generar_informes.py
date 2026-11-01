"""Trusted document generator: reads academic Markdown/templates, never runs app code."""
from pathlib import Path
import json
import re
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "documentos-academicos"
SHARED = Path(__file__).resolve().parent
QUARTO = shutil.which("quarto")
if not QUARTO:
    raise RuntimeError("Quarto/Pandoc is required for document conversion only")

ITEMS = [
    ("ACD4-semana-4", "ACD4_MODELO_AMENAZAS", 4),
    ("APE4-semana-4", "APE4_SEGURIDAD_VALIDACIONES_PRUEBAS", 4),
    ("AA4-semana-4", "AA4_INFORME_TECNICO_SEGURIDAD", 4),
    ("ACD5-semana-5", "ACD5_INTEGRACION_DESPLIEGUE_ESTANDARES", 5),
    ("APE5-semana-5", "APE5_INTEGRACION_CONTENEDORES_DESPLIEGUE", 5),
    ("AA5-semana-5", "AA5_DOSSIER_TECNICO_Y_REFLEXION", 5),
]


def pandoc(text, source, target, chapters=False):
    args = [QUARTO, "pandoc", "-f", source, "-t", target]
    if chapters:
        args.append("--top-level-division=chapter")
    return subprocess.run(args, input=text, encoding="utf-8", capture_output=True, check=True).stdout


def cell_text(cell):
    result = []
    for block in cell[4]:
        if block["t"] in ("Plain", "Para"):
            result += block["c"] + [{"t": "Space"}]
    return result


def transform(node, prefix):
    if isinstance(node, list):
        return [transform(item, prefix) for item in node]
    if not isinstance(node, dict):
        return node
    kind, content = node.get("t"), node.get("c")
    if kind == "Table":
        header_rows = content[3][1]
        names = [cell_text(cell) for cell in header_rows[0][1]] if header_rows else []
        rows, blocks = [], []
        for body in content[4]:
            rows += body[2] + body[3]
        for row in rows:
            for index, cell in enumerate(row[1]):
                label = names[index] if index < len(names) else []
                blocks.append({"t": "Para", "c": [{"t": "Strong", "c": label},
                               {"t": "Str", "c": ": "}] + cell_text(cell)})
            blocks.append({"t": "RawBlock", "c": ["latex", r"\medskip\hrule\medskip"]})
        return transform({"t": "Div", "c": [["", [], []], blocks]}, prefix)
    if kind == "Code":
        sep = next(value for value in "|!+;" if value not in content[1])
        return {"t": "RawInline", "c": ["latex", "\\path" + sep + content[1] + sep]}
    if kind == "CodeBlock":
        return {"t": "RawBlock", "c": ["latex",
                "\\begin{Verbatim}[breaklines=true,breakanywhere=true,fontsize=\\footnotesize]\n"
                + content[1] + "\n\\end{Verbatim}"]}
    if kind == "Header":
        content[1][0] = prefix + "-" + content[1][0]
    return {key: transform(value, prefix) for key, value in node.items()}


def convert(path, prefix, chapters=False, main=False, ape=False, annex=False):
    ast = json.loads(pandoc(path.read_text(encoding="utf-8"), "gfm", "json"))
    blocks, started = [], not ape
    for block in ast["blocks"]:
        if block["t"] == "Header":
            level, _, title = block["c"]
            words = " ".join(item.get("c", "") for item in title if item["t"] == "Str")
            if ape and level == 2 and words.startswith("Objetivo"):
                started = True
            if main and level == 1:
                continue
            if started:
                block["c"][0] = level + 1 if annex and ape else level - 1 if main else level
        if started:
            blocks.append(block)
    ast["blocks"] = blocks
    return pandoc(json.dumps(transform(ast, prefix)), "json", "latex", chapters)


def escape(text):
    return "".join({"&": r"\&", "%": r"\%", "_": r"\_", "#": r"\#"}.get(c, c) for c in text)


def aa_prefix(kind, title, week):
    baseline = (ROOT / ".agents/skills/informes-latex-unl/assets/FORMATO.tex").read_text(encoding="utf-8")
    prefix = baseline[:re.search(r"^\\chapter\{", baseline, re.M).start()]
    prefix = prefix.replace(r"\usepackage{hyperref}", "\\usepackage{hyperref}\n\\usepackage{calc}\n\\usepackage{xurl}\n\\usepackage{fvextra}\n\\providecommand{\\tightlist}{\\setlength{\\itemsep}{0pt}\\setlength{\\parskip}{0pt}}")
    prefix = prefix.replace(r"\includegraphics[width=\paperwidth,height=\paperheight]{fondo.jpeg}", r"\IfFileExists{fondo.jpeg}{\includegraphics[width=\paperwidth,height=\paperheight]{fondo.jpeg}}{\parbox{0.8\paperwidth}{\centering [PENDIENTE: fondo.jpeg original]}}")
    prefix = prefix.replace(r"\includegraphics[height=1.2cm]{logo_unl.jpeg}", r"\IfFileExists{logo_unl.jpeg}{\includegraphics[height=1.2cm]{logo_unl.jpeg}}{\fbox{\scriptsize PENDIENTE: logo\_unl.jpeg}}")
    values = {"TituloTrabajo": title, "Autor": "[PENDIENTE: estudiante/integrantes reales]",
              "Director": "Edison Leonardo Coronel Romero (referencia APE4; confirmar)",
              "Fecha": f"Semana {week} -- [PENDIENTE: fecha real]"}
    for command, value in values.items():
        pattern = r"\\newcommand\{\\" + command + r"\}\{[^\n]*\}"
        prefix = re.sub(pattern, lambda _: "\\newcommand{\\" + command + "}{" + escape(value) + "}", prefix)
    prefix = prefix.replace("Complejidad Computacional.", "Desarrollo Basado en Plataformas (referencia APE4; confirmar).")
    start = prefix.index(r"\noindent Los autores declaran")
    end = prefix.index(r"\newpage", start)
    declaration = ("Se utilizó ChatGPT/Codex para inspección de fuentes, consulta oficial y preparación de documentos, "
                   "tests y configuración propuestos. No se ejecutó código del target, no hay parche funcional "
                   "o despliegue nuevos ni participación/aceptación humana atribuida.\n\n"
                   "\\noindent\\textbf{Declaración enviada por: [PENDIENTE: persona real].}\n")
    return prefix[:start] + declaration + prefix[end:]


def ape_prefix(number, title, week):
    source = (ROOT / ".agents/skills/informes-latex-unl/assets/FORMATOAPE.tex").read_text(encoding="utf-8")
    prefix = source[:source.index(r"\end{tabularx}") + len(r"\end{tabularx}")]
    prefix = prefix.replace("Nro. 004", f"Nro. {number:03}")
    prefix = re.sub(r"\{\\large [^\n]+", lambda _: "{\\large " + escape(title.split("—", 1)[-1].strip()) + "}", prefix, count=1)
    prefix = prefix.replace(r"\begin{document}", "\\usepackage{calc}\n\\usepackage{fvextra}\n\\providecommand{\\tightlist}{\\setlength{\\itemsep}{0pt}\\setlength{\\parskip}{0pt}}\n\\begin{document}")
    result = ("Discute cómo los estándares Web influyen en el desarrollo de software, bajo los principios "
              "de solidaridad, transparencia, responsabilidad y honestidad.")
    fields = {
        "Nombre del estudiante(s)": "[PENDIENTE: integrantes reales]",
        "Asignatura": "Desarrollo Basado en Plataformas" + (" (referencia APE4; confirmar)" if number == 5 else ""),
        "Ciclo": "5 A" + (" (referencia APE4)" if number == 5 else ""),
        "Unidad": "1" + (" (referencia APE4)" if number == 5 else ""),
        "Resultado de aprendizaje": result if number == 4 else "No aportado específicamente en la guía resumida de APE5.",
        "Práctica Nro.": f"{number:03}", "Título de la Práctica": title.split("—", 1)[-1].strip(),
        "Nombre del Docente": "Edison Leonardo Coronel Romero" + (" (referencia APE4; confirmar)" if number == 5 else ""),
        "Fecha": f"Semana {week}; [PENDIENTE: fecha real]",
        "Horario": "Por definir según horario académico" if number == 4 else "No especificado en la guía resumida; pendiente.",
        "Lugar": "Aula / laboratorio asignado" if number == 4 else "No especificado en la guía resumida; pendiente.",
        "Tiempo planificado en el Sílabo": "3 horas",
    }
    for label, value in fields.items():
        pattern = r"(\\textbf\{" + re.escape(label) + r"\} & ).*?( \\\\ \\hline)"
        prefix, count = re.subn(pattern, lambda m: m[1] + escape(value) + m[2], prefix)
        if count != 1:
            raise AssertionError("Institutional field missing: " + label)
    return prefix + "\n\\textbf{Estado:} preparación documental y artefactos; ejecución funcional, corrección y despliegue pendientes.\\par\n"


REFERENCES = [
    ("owaspauth", "OWASP", "Authentication Cheat Sheet", "https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html"),
    ("owaspauthz", "OWASP", "Authorization Cheat Sheet", "https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html"),
    ("owaspapi", "OWASP", "API1:2023 Broken Object Level Authorization", "https://api-security.owasp.org/editions/2023/en/0xa1-broken-object-level-authorization/"),
    ("docker", "Docker", "Compose services and networks", "https://docs.docker.com/reference/compose-file/services/"),
    ("postgres", "Docker Library", "PostgreSQL official image documentation", "https://raw.githubusercontent.com/docker-library/docs/master/postgres/README.md"),
    ("spring", "Spring Boot", "Container Images", "https://docs.spring.io/spring-boot/reference/packaging/container-images/index.html"),
    ("http", "IETF", "RFC 9110 HTTP Semantics", "https://www.rfc-editor.org/rfc/rfc9110.html"),
    ("pytest", "pytest", "Skipping and xfail", "https://docs.pytest.org/en/stable/how-to/skipping.html"),
]


def main():
    bibliography = "\n\n".join("@misc{" + key + ",\n author = {{" + author + "}},\n title = {"
                  + title + "},\n howpublished = {\\url{" + url + "}}\n}" for key, author, title, url in REFERENCES) + "\n"
    for folder, basename, week in ITEMS:
        target = BASE / folder
        source = target / (basename + ".md")
        title = source.read_text(encoding="utf-8").splitlines()[0].removeprefix("# ")
        ape = folder.startswith("APE")
        body = convert(source, folder, chapters=not ape, main=True, ape=ape)
        if ape:
            prefix = ape_prefix(week, title, week)
            # Original eleven-section order: append supporting material within Anexos.
            at = body.rfind(r"\section{")
            annexes = [target / ("PLAN_PRUEBAS.md" if week == 4 else "INSTRUCCIONES_DESPLIEGUE.md"),
                       SHARED / "HALLAZGOS_Y_TRAZABILIDAD.md", SHARED / "FUENTES.md", target / "NOTA_INDIVIDUAL.md"]
            annex = ""
            for index, path in enumerate(annexes):
                annex += convert(path, f"{folder}-anexo{index}", ape=True, annex=True)
            codefiles = [target / "pruebas/test_seguridad_api.py"] if week == 4 else [target / "despliegue/Dockerfile", target / "despliegue/compose.yaml"]
            for path in codefiles:
                annex += "\n\\subsection{Fuente propuesta: " + escape(path.name) + "}\n\\begin{Verbatim}[breaklines=true,breakanywhere=true,fontsize=\\footnotesize]\n" + path.read_text(encoding="utf-8") + "\n\\end{Verbatim}\n"
            body = body[:at] + annex + body[at:]
            ref = body.index(r"\section{Anexos}")
            body = body[:ref] + "\\nocite{*}\n\\bibliographystyle{IEEEtran}\n\\bibliography{referencias}\n" + body[ref:]
            shutil.copyfile(ROOT / ".agents/skills/informes-latex-unl/assets/reporte_tecnico_overleaf_logo.png", target / "reporte_tecnico_overleaf_logo.png")
        else:
            prefix = aa_prefix(folder.split("-")[0], title, week)
            body += "\n\\nocite{*}\n\\bibliographystyle{IEEEtran}\n\\bibliography{referencias}\n\\appendix\n"
            annexes = ([target / "MATRIZ_CIERRE.md", target / "BITACORA.md"] if folder.startswith("AA5") else
                       [SHARED / "MATRIZ_AMENAZAS.md", SHARED / "HALLAZGOS_Y_TRAZABILIDAD.md"] if week == 4 else
                       [BASE / "APE5-semana-5/INSTRUCCIONES_DESPLIEGUE.md", BASE / "AA5-semana-5/MATRIZ_CIERRE.md"])
            for index, path in enumerate(annexes + [SHARED / "FUENTES.md", target / "NOTA_INDIVIDUAL.md"]):
                body += convert(path, f"{folder}-anexo{index}", chapters=True)
        tex = prefix + "\n" + body + "\n\\end{document}\n"
        for char, command in [("→", r"\ensuremath{\rightarrow}"), ("↔", r"\ensuremath{\leftrightarrow}"), ("×", r"\ensuremath{\times}"), ("≤", r"\ensuremath{\leq}"), ("≥", r"\ensuremath{\geq}")]:
            tex = tex.replace(char, command)
        (target / (basename + ".tex")).write_text(tex, encoding="utf-8")
        (target / "referencias.bib").write_text(bibliography, encoding="utf-8")
        print(folder + ": generated institutional report, not compiled")


if __name__ == "__main__":
    main()
