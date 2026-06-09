from __future__ import annotations

import json
import re
import time
import unicodedata
from urllib.parse import quote

import requests
from bs4 import BeautifulSoup


BASE_URL = "https://www.dicio.com.br"
TIMEOUT_SECONDS = 10
RATE_LIMIT_SECONDS = 1.2
TRIM_CHARS = " .:;-–"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/125.0 Safari/537.36"
    ),
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "pt-BR,pt;q=0.9,en;q=0.7",
}


def _resultado_vazio(palavra: str, url: str) -> dict:
    return {
        "palavra": palavra or None,
        "url": url or None,
        "significado": None,
        "classe_gramatical": None,
        "separacao_silabica": None,
        "sinonimos": [],
        "etimologia": None,
        "fonte": "Dicio",
    }


def _normalizar_para_url(palavra: str) -> str:
    texto = unicodedata.normalize("NFKD", palavra.strip().lower())
    texto = "".join(char for char in texto if not unicodedata.combining(char))
    texto = re.sub(r"[^a-z0-9\s-]", "", texto)
    texto = re.sub(r"[\s_-]+", "-", texto).strip("-")
    return quote(texto)


def _texto_limpo(elemento) -> str | None:
    if not elemento:
        return None
    texto = elemento.get_text(" ", strip=True)
    texto = re.sub(r"\s+", " ", texto).strip()
    return texto or None


def _todos_os_textos(soup: BeautifulSoup) -> list[str]:
    for tag in soup(["script", "style", "noscript", "svg"]):
        tag.decompose()
    textos = []
    for elemento in soup.find_all(["h1", "h2", "h3", "p", "li", "span"]):
        texto = _texto_limpo(elemento)
        if texto and texto not in textos:
            textos.append(texto)
    return textos


def _extrair_palavra(soup: BeautifulSoup, fallback: str) -> str | None:
    h1 = soup.find("h1")
    texto = _texto_limpo(h1)
    if not texto:
        return fallback or None
    texto = re.sub(r"^significado\s+de\s+", "", texto, flags=re.I)
    return texto.strip(" :") or fallback or None


def _extrair_classe_gramatical(textos: list[str]) -> str | None:
    for texto in textos:
        match = re.search(r"classe\s+gramatical\s*:\s*(.+)", texto, flags=re.I)
        if match:
            valor = re.split(r"\s+separação\s+silábica\s*:", match.group(1), flags=re.I)[0]
            return valor.strip(TRIM_CHARS).lower() or None

    padroes = [
        r"\bsubstantivo\s+(?:masculino|feminino|comum|de dois gêneros)\b",
        r"\badjetivo\b",
        r"\bverbo\b",
        r"\badvérbio\b",
        r"\bpronome\b",
        r"\bpreposição\b",
        r"\bconjunção\b",
        r"\binterjeição\b",
        r"\bartigo\b",
        r"\bnumeral\b",
    ]
    for texto in textos:
        for padrao in padroes:
            match = re.search(padrao, texto, flags=re.I)
            if match:
                return match.group(0).lower()
    return None


def _extrair_bloco_significado(soup: BeautifulSoup) -> str | None:
    titulo = soup.find(string=re.compile(r"significado\s+de", re.I))
    if titulo:
        for elemento in titulo.find_all_next(["p", "li"], limit=8):
            texto = _texto_limpo(elemento)
            if texto and len(texto) > 30 and "classe gramatical" not in texto.lower():
                return texto

    for elemento in soup.find_all(["p", "li"]):
        texto = _texto_limpo(elemento)
        if texto and len(texto) > 30 and re.search(r"\b(substantivo|adjetivo|verbo|advérbio)\b", texto, re.I):
            return texto
    return None


def _extrair_significado(soup: BeautifulSoup, classe: str | None) -> str | None:
    texto = _extrair_bloco_significado(soup)
    if not texto:
        return None

    texto = re.split(r"\bEtimologia\b|\bSinônimos?\b|\bDefinição\b", texto, flags=re.I)[0]
    if classe:
        texto = re.sub(re.escape(classe), "", texto, count=1, flags=re.I)
    texto = re.sub(
        r"^\s*(?:adjetivo|substantivo|verbo|advérbio|pronome|preposição|conjunção)"
        r"(?:\s+e\s+(?:adjetivo|substantivo|verbo|advérbio|pronome))*"
        r"(?:\s+(?:masculino|feminino|comum|de dois gêneros))*\s*",
        "",
        texto,
        flags=re.I,
    )
    texto = texto.strip(TRIM_CHARS)
    partes = re.split(r"(?<=[.!?])\s+", texto)
    return partes[0].strip() if partes and partes[0].strip() else None


def _extrair_por_rotulo(textos: list[str], rotulos: tuple[str, ...]) -> str | None:
    for texto in textos:
        for rotulo in rotulos:
            padrao = rf"{re.escape(rotulo)}\s*[:：-]?\s*(.+?)(?:\s+(?:Classe gramatical|Separação silábica|Plural|Feminino|Masculino|Frases com|Exemplos com|Sinônimos?|Antônimos?|Definição de)\b|$)"
            match = re.search(padrao, texto, flags=re.I)
            if match:
                valor = match.group(1).strip(TRIM_CHARS)
                return valor or None
    return None


def _extrair_sinonimos(soup: BeautifulSoup, palavra: str | None) -> list[str]:
    titulo = soup.find(string=re.compile(r"^\s*sinônimos?\s+de\s+", re.I))
    texto = None
    if titulo:
        bloco = titulo.find_parent(["section", "div", "p"])
        texto = _texto_limpo(bloco)
    if not texto:
        texto = _extrair_por_rotulo(_todos_os_textos(soup), ("sinônimos", "sinônimo"))
    if not texto:
        return []

    if palavra:
        texto = re.sub(rf"^sinônimos?\s+de\s+{re.escape(palavra)}", "", texto, flags=re.I).strip()
        texto = re.sub(rf"^{re.escape(palavra)}\s+é\s+sinônimo\s+de\s*:?", "", texto, flags=re.I).strip()
    texto = re.sub(r"\bver também\b.*$", "", texto, flags=re.I)
    texto = re.split(r"\bantônimos?\b|\bdefinição\b|\bfrases com\b|\bexemplos com\b", texto, flags=re.I)[0]
    partes = re.split(r",|;|\se\s", texto)
    sinonimos = []
    vistos = set()
    for parte in partes:
        item = parte.strip(TRIM_CHARS).lower()
        if item and item not in vistos and len(item) <= 60:
            vistos.add(item)
            sinonimos.append(item)
    return sinonimos


def _extrair_etimologia(soup: BeautifulSoup, textos: list[str]) -> str | None:
    marcador = soup.find(string=re.compile(r"\bEtimologia\b", re.I))
    if marcador:
        texto = _texto_limpo(marcador.parent)
        if texto:
            return re.sub(r"^etimologia\s*", "", texto, flags=re.I).strip(TRIM_CHARS) or None
    return _extrair_por_rotulo(textos, ("etimologia", "origem da palavra"))


def _parsear_html(html: str, palavra_original: str, url: str) -> dict:
    soup = BeautifulSoup(html, "html.parser")
    textos = _todos_os_textos(soup)
    classe = _extrair_classe_gramatical(textos)
    palavra = _extrair_palavra(soup, palavra_original)

    return {
        "palavra": palavra,
        "url": url,
        "significado": _extrair_significado(soup, classe),
        "classe_gramatical": classe,
        "separacao_silabica": _extrair_por_rotulo(textos, ("separação silábica", "separação silábica de")),
        "sinonimos": _extrair_sinonimos(soup, palavra),
        "etimologia": _extrair_etimologia(soup, textos),
        "fonte": "Dicio",
    }


def buscar_palavra_dicio(palavra: str) -> dict:
    palavra_original = " ".join((palavra or "").split())
    slug = _normalizar_para_url(palavra_original)
    url = f"{BASE_URL}/{slug}/" if slug else BASE_URL
    resultado = _resultado_vazio(palavra_original, url)

    if not slug:
        return resultado

    try:
        resposta = requests.get(url, headers=HEADERS, timeout=TIMEOUT_SECONDS)
    except requests.RequestException:
        return resultado

    if resposta.status_code == 404:
        return resultado

    if not resposta.ok:
        return resultado

    return _parsear_html(resposta.text, palavra_original, url)


def buscar_varias_palavras(lista_palavras: list[str]) -> list[dict]:
    resultados = []
    for indice, palavra in enumerate(lista_palavras):
        if indice > 0:
            time.sleep(RATE_LIMIT_SECONDS)
        resultados.append(buscar_palavra_dicio(palavra))
    return resultados


if __name__ == "__main__":
    palavras = ["amor", "cabelo", "voluntário"]
    resultado = buscar_varias_palavras(palavras)
    print(json.dumps(resultado, ensure_ascii=False, indent=2))