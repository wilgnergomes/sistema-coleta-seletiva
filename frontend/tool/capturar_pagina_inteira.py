"""Captura a página inteira de rotas do build Web (Chrome headless, CDP).

O Flutter Web desenha num canvas do tamanho da janela, então a captura usa
uma janela alta o bastante para toda a página e depois corta o fundo vazio
abaixo do conteúdo. Também registra estados de interação (hover) quando
coordenadas são informadas.

Uso, com o build servido pelo Caddy:
  python -m pip install websocket-client==1.8.0 pillow==11.3.0
  python tool/capturar_pagina_inteira.py --chrome "C:/Program Files/Google/Chrome/Application/chrome.exe" \
      --url http://127.0.0.1:8000/ --saida capturas --prefixo depois
"""

import argparse
import base64
import io
import json
import os
import subprocess
import tempfile
import time
import urllib.request

import websocket
from PIL import Image

LARGURAS = [390, 768, 1366]
ALTURA_JANELA = {390: 844, 768: 1024, 1366: 768}


class Cdp:
    def __init__(self, url):
        self.ws = websocket.create_connection(url, timeout=90)
        self.n = 0

    def chamar(self, metodo, **params):
        self.n += 1
        self.ws.send(json.dumps({"id": self.n, "method": metodo, "params": params}))
        while True:
            m = json.loads(self.ws.recv())
            if m.get("id") == self.n:
                if "error" in m:
                    raise RuntimeError(f"{metodo}: {m['error']}")
                return m.get("result", {})


def cortar_fundo(png, cores_fundo=((247, 249, 247), (255, 255, 255))):
    """Remove as linhas finais que só têm a cor de fundo."""
    img = Image.open(io.BytesIO(png)).convert("RGB")
    largura, altura = img.size
    pixels = img.load()
    fim = altura
    while fim > 1:
        linha = {pixels[x, fim - 1] for x in range(0, largura, max(1, largura // 64))}
        if not linha.issubset(set(cores_fundo)):
            break
        fim -= 1
    return img.crop((0, 0, largura, min(altura, fim + 24)))


def capturar(cdp, url, largura, altura, espera=6.0):
    cdp.chamar("Emulation.setDeviceMetricsOverride", width=largura, height=altura, deviceScaleFactor=1, mobile=largura < 600)
    cdp.chamar("Page.navigate", url=url)
    time.sleep(espera)
    return base64.b64decode(cdp.chamar("Page.captureScreenshot", format="png")["data"])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--chrome", required=True)
    ap.add_argument("--url", default="http://127.0.0.1:8000/")
    ap.add_argument("--saida", default="capturas")
    ap.add_argument("--prefixo", default="pagina")
    ap.add_argument("--rota", default="/")
    ap.add_argument("--altura-total", type=int, default=7000)
    ap.add_argument("--hover", nargs=2, type=int, help="x y para registrar o hover em 1366 px")
    args = ap.parse_args()
    os.makedirs(args.saida, exist_ok=True)

    perfil = tempfile.mkdtemp(prefix="pagina-inteira-")
    proc = subprocess.Popen([args.chrome, "--headless=new", "--remote-debugging-port=9335", f"--user-data-dir={perfil}",
                             "--no-first-run", "about:blank"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        for _ in range(50):
            try:
                alvo = next(a for a in json.load(urllib.request.urlopen("http://127.0.0.1:9335/json")) if a["type"] == "page")
                break
            except Exception:
                time.sleep(0.2)
        cdp = Cdp(alvo["webSocketDebuggerUrl"])
        cdp.chamar("Page.enable")
        for largura in LARGURAS:
            png = capturar(cdp, f"{args.url}?c={time.time_ns()}#{args.rota}", largura, args.altura_total)
            img = cortar_fundo(png)
            arquivo = os.path.join(args.saida, f"{args.prefixo}-{largura}-inteira.png")
            img.save(arquivo, optimize=True)
            dobra = capturar(cdp, f"{args.url}?c={time.time_ns()}#{args.rota}", largura, ALTURA_JANELA[largura], 4.0)
            arquivo_dobra = os.path.join(args.saida, f"{args.prefixo}-{largura}-dobra.png")
            Image.open(io.BytesIO(dobra)).save(arquivo_dobra, optimize=True)
            print(json.dumps({"largura": largura, "inteira": arquivo, "altura": img.size[1], "dobra": arquivo_dobra}), flush=True)
        if args.hover:
            x, y = args.hover
            capturar(cdp, f"{args.url}?c={time.time_ns()}#{args.rota}", 1366, 768, 4.0)
            cdp.chamar("Input.dispatchMouseEvent", type="mouseMoved", x=x, y=y)
            time.sleep(0.6)
            png = base64.b64decode(cdp.chamar("Page.captureScreenshot", format="png")["data"])
            arquivo = os.path.join(args.saida, f"{args.prefixo}-1366-hover.png")
            Image.open(io.BytesIO(png)).save(arquivo, optimize=True)
            print(json.dumps({"hover": arquivo}), flush=True)
    finally:
        proc.terminate()
        proc.wait(timeout=10)


if __name__ == "__main__":
    main()
