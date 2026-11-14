"""Grava transições do build Web em quadros (Chrome headless, CDP screencast).

Capturas estáticas não mostram movimento. Esta ferramenta clica num ponto da
tela enquanto grava os quadros que o Chrome compõe e salva, para cada passo:

- `{nome}.gif`: a gravação curta da transição;
- `{nome}-quadros.png`: uma folha com quadros intermediários e o tempo de cada um.

Uso, com o build de demonstração servido pelo Caddy:
  python -m pip install websocket-client==1.8.0 pillow==11.3.0
  python tool/gravar_transicoes.py --chrome "C:/Program Files/Google/Chrome/Application/chrome.exe" \\
      --largura 1366 --rota / --saida capturas \\
      --passo inicio-pontos:868,32 --passo pontos-materiais:950,32

Cada passo é `nome:x,y`; cliques preparatórios (abrir um menu, por exemplo)
vêm antes, separados por `+`: `nome:350,28+300,120`. `--sondar` só salva uma
captura da tela inicial, para localizar as coordenadas. `--entrar
morador:3:/morador/inicio` entra na demonstração antes dos passos.
`--tabular nome:N` pressiona Tab N vezes e captura o foco do teclado.
`--pairar nome:x,y` salva uma captura com o mouse parado no ponto (hover).
`--reduzido` emula `prefers-reduced-motion: reduce`; sem ele, a ferramenta
declara `no-preference`, porque o Chrome headless informa movimento reduzido.
"""

import argparse
import base64
import io
import json
import os
import subprocess
import tempfile
import time
import urllib.parse
import urllib.request

import websocket
from PIL import Image, ImageDraw

ALTURA = {390: 844, 768: 1024, 1366: 768}


class Cdp:
    def __init__(self, url):
        self.ws = websocket.create_connection(url, timeout=90)
        self.n = 0
        self.quadros = []

    def enviar(self, metodo, **params):
        self.n += 1
        self.ws.send(json.dumps({"id": self.n, "method": metodo, "params": params}))
        return self.n

    def _tratar(self, m):
        if m.get("method") == "Page.screencastFrame":
            p = m["params"]
            self.quadros.append((p["metadata"]["timestamp"], p["data"]))
            self.enviar("Page.screencastFrameAck", sessionId=p["sessionId"])

    def chamar(self, metodo, **params):
        n = self.enviar(metodo, **params)
        while True:
            m = json.loads(self.ws.recv())
            self._tratar(m)
            if m.get("id") == n:
                if "error" in m:
                    raise RuntimeError(f"{metodo}: {m['error']}")
                return m.get("result", {})

    def aguardar(self, segundos):
        fim = time.time() + segundos
        self.ws.settimeout(0.05)
        try:
            while time.time() < fim:
                try:
                    self._tratar(json.loads(self.ws.recv()))
                except websocket.WebSocketTimeoutException:
                    pass
        finally:
            self.ws.settimeout(90)

    def clicar(self, x, y):
        self.chamar("Input.dispatchMouseEvent", type="mouseMoved", x=x, y=y)
        for tipo in ("mousePressed", "mouseReleased"):
            self.chamar("Input.dispatchMouseEvent", type=tipo, x=x, y=y, button="left", clickCount=1)


def entrar(cdp, base, perfil, ident, retorno):
    cdp.chamar("Page.navigate", url=f"{base}?c={time.time_ns()}#/demonstracao?perfil={perfil}&retorno={urllib.parse.quote(retorno, safe='')}")
    cdp.aguardar(3)
    if ident:
        cdp.chamar("Input.insertText", text=ident)
        cdp.aguardar(0.3)
    for tipo in ("rawKeyDown", "keyUp"):
        cdp.chamar("Input.dispatchKeyEvent", type=tipo, key="Enter", code="Enter", windowsVirtualKeyCode=13)
    cdp.aguardar(4)


def folha(quadros, inicio, arquivo, janela=360, escala=0.34, quantos=6):
    """Quadros intermediários lado a lado, com o tempo desde o clique."""
    alvos = [i * janela / (quantos - 1) for i in range(quantos)]
    escolhidos = []
    for alvo in alvos:
        t, img = min(quadros, key=lambda q: abs((q[0] - inicio) * 1000 - alvo))
        if not escolhidos or escolhidos[-1][0] != t:
            escolhidos.append((t, img))
    largura, altura = escolhidos[0][1].size
    w, h = int(largura * escala), int(altura * escala)
    margem, faixa = 8, 26
    saida = Image.new("RGB", (len(escolhidos) * (w + margem) + margem, h + faixa + margem), (36, 53, 45))
    desenho = ImageDraw.Draw(saida)
    for i, (t, img) in enumerate(escolhidos):
        x = margem + i * (w + margem)
        saida.paste(img.resize((w, h)), (x, faixa))
        desenho.text((x + 4, 6), f"{(t - inicio) * 1000:.0f} ms", fill=(255, 255, 255))
    saida.save(arquivo, optimize=True)


def gravar_passo(cdp, nome, cliques, saida, duracao=0.9, janela=360):
    for x, y in cliques[:-1]:
        cdp.clicar(x, y)
        cdp.aguardar(0.7)
    cdp.quadros = []
    cdp.chamar("Page.startScreencast", format="jpeg", quality=80, everyNthFrame=1)
    cdp.aguardar(0.25)
    inicio = time.time()
    cdp.clicar(*cliques[-1])
    cdp.aguardar(duracao)
    cdp.chamar("Page.stopScreencast")
    cdp.aguardar(0.2)
    quadros = [(t, Image.open(io.BytesIO(base64.b64decode(d))).convert("RGB")) for t, d in cdp.quadros]
    # Quadros a partir de pouco antes do clique.
    quadros = [q for q in quadros if q[0] >= inicio - 0.05]
    if len(quadros) < 2:
        print(json.dumps({"passo": nome, "erro": "poucos quadros"}), flush=True)
        return
    gif = os.path.join(saida, f"{nome}.gif")
    duracoes = [max(20, int((quadros[i + 1][0] - quadros[i][0]) * 1000)) for i in range(len(quadros) - 1)] + [600]
    reduzidos = [img.resize((img.width // 2, img.height // 2)) for _, img in quadros]
    reduzidos[0].save(gif, save_all=True, append_images=reduzidos[1:], duration=duracoes, loop=0, optimize=True)
    folha(quadros, inicio, os.path.join(saida, f"{nome}-quadros.png"), janela)
    print(json.dumps({"passo": nome, "quadros": len(quadros), "gif": gif}), flush=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--chrome", required=True)
    ap.add_argument("--url", default="http://127.0.0.1:8000/")
    ap.add_argument("--largura", type=int, default=1366)
    ap.add_argument("--rota", default="/")
    ap.add_argument("--saida", default="capturas")
    ap.add_argument("--passo", action="append", default=[])
    ap.add_argument("--entrar", help="perfil:id:retorno")
    ap.add_argument("--sondar", action="store_true")
    ap.add_argument("--reduzido", action="store_true")
    ap.add_argument("--duracao", type=float, default=0.9)
    ap.add_argument("--janela", type=int, default=360, help="ms cobertos pela folha de quadros")
    ap.add_argument("--tabular", action="append", default=[], help="nome:N — pressiona Tab N vezes e captura o foco")
    ap.add_argument("--pairar", action="append", default=[], help="nome:x,y — captura com o mouse parado no ponto")
    args = ap.parse_args()
    os.makedirs(args.saida, exist_ok=True)

    perfil = tempfile.mkdtemp(prefix="transicoes-")
    proc = subprocess.Popen([args.chrome, "--headless=new", "--remote-debugging-port=9336", f"--user-data-dir={perfil}",
                             "--no-first-run", "about:blank"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        for _ in range(50):
            try:
                alvo = next(a for a in json.load(urllib.request.urlopen("http://127.0.0.1:9336/json")) if a["type"] == "page")
                break
            except Exception:
                time.sleep(0.2)
        cdp = Cdp(alvo["webSocketDebuggerUrl"])
        cdp.chamar("Page.enable")
        altura = ALTURA.get(args.largura, 900)
        cdp.chamar("Emulation.setDeviceMetricsOverride", width=args.largura, height=altura, deviceScaleFactor=1,
                   mobile=args.largura < 600)
        # O Chrome headless informa movimento reduzido por padrão, e o app o
        # respeita (troca sem animação). A preferência é declarada sempre.
        preferencia = "reduce" if args.reduzido else "no-preference"
        cdp.chamar("Emulation.setEmulatedMedia", features=[{"name": "prefers-reduced-motion", "value": preferencia}])
        if args.entrar:
            # A seleção leva direto ao retorno informado.
            p, ident, retorno = args.entrar.split(":", 2)
            entrar(cdp, args.url, p, ident, retorno)
        else:
            cdp.chamar("Page.navigate", url=f"{args.url}?c={time.time_ns()}#{args.rota}")
            cdp.aguardar(5)
        if args.sondar:
            png = base64.b64decode(cdp.chamar("Page.captureScreenshot", format="png")["data"])
            arquivo = os.path.join(args.saida, f"sonda-{args.largura}.png")
            open(arquivo, "wb").write(png)
            print(json.dumps({"sonda": arquivo}), flush=True)
        for passo in args.passo:
            nome, coordenadas = passo.split(":", 1)
            cliques = [tuple(int(v) for v in c.split(",")) for c in coordenadas.split("+")]
            gravar_passo(cdp, nome, cliques, args.saida, args.duracao, args.janela)
            cdp.aguardar(0.8)
        for passo in args.tabular:
            nome, vezes = passo.split(":", 1)
            for _ in range(int(vezes)):
                for tipo in ("rawKeyDown", "keyUp"):
                    cdp.chamar("Input.dispatchKeyEvent", type=tipo, key="Tab", code="Tab", windowsVirtualKeyCode=9)
                cdp.aguardar(0.15)
            cdp.aguardar(0.5)
            png = base64.b64decode(cdp.chamar("Page.captureScreenshot", format="png")["data"])
            arquivo = os.path.join(args.saida, f"{nome}.png")
            open(arquivo, "wb").write(png)
            print(json.dumps({"tabular": arquivo}), flush=True)
        for passo in args.pairar:
            nome, coordenadas = passo.split(":", 1)
            x, y = (int(v) for v in coordenadas.split(","))
            cdp.chamar("Input.dispatchMouseEvent", type="mouseMoved", x=x, y=y)
            cdp.aguardar(0.6)
            png = base64.b64decode(cdp.chamar("Page.captureScreenshot", format="png")["data"])
            arquivo = os.path.join(args.saida, f"{nome}.png")
            open(arquivo, "wb").write(png)
            print(json.dumps({"pairar": arquivo}), flush=True)
    finally:
        proc.terminate()
        proc.wait(timeout=10)


if __name__ == "__main__":
    main()
