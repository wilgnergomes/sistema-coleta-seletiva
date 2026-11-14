"""Captura telas do build Web em larguras de referência (Chrome headless, CDP).

Para cada largura, abre as rotas indicadas e salva um PNG da página inteira
visível. Para áreas restritas, entra pela tela de demonstração usando a
árvore de acessibilidade do Flutter Web (sem login real: identidade simulada).

Uso, com o Caddy servindo o build em http://127.0.0.1:8000:
  python -m pip install websocket-client==1.8.0
  python tool/capturar_telas.py --chrome "C:/Program Files/Google/Chrome/Application/chrome.exe" --saida capturas
"""

import argparse
import base64
import json
import os
import subprocess
import tempfile
import time
import urllib.request

import websocket

LARGURAS = [(360, 780), (390, 844), (768, 1024), (1366, 768)]
if os.environ.get('SO_LARGURA'):
    LARGURAS = [l for l in LARGURAS if str(l[0]) == os.environ['SO_LARGURA']]

# (nome, rota, perfil, identificador)
ROTAS_TODAS = [
    ("inicio", "/", None, None),
    ("pontos", "/pontos", None, None),
    ("materiais", "/materiais", None, None),
    ("morador-inicio", "/morador/inicio", "morador", "3"),
    ("operador-fila", "/operador/fila/3?situacao=TODAS", "operador", "1"),
    ("admin-moradores", "/administracao/moradores", "administrador", None),
]
ROTAS = [r for r in ROTAS_TODAS if not os.environ.get('SO_RESTRITAS') or r[2]]


class Cdp:
    def __init__(self, url):
        self.ws = websocket.create_connection(url, timeout=60)
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

    def js(self, expr):
        return self.chamar("Runtime.evaluate", expression=expr, returnByValue=True, awaitPromise=True).get("result", {}).get("value")

    def clicar(self, x, y):
        for tipo in ("mousePressed", "mouseReleased"):
            self.chamar("Input.dispatchMouseEvent", type=tipo, x=x, y=y, button="left", clickCount=1)


def _centro_nao_usado(cdp, seletor_js):
    """Centro de um nó da árvore de semântica do Flutter Web."""
    return cdp.js(f"""(() => {{
        const el = {seletor_js};
        if (!el) return null;
        const r = el.getBoundingClientRect();
        return [r.x + r.width / 2, r.y + r.height / 2];
    }})()""")


def entrar(cdp, base, perfil, ident, retorno):
    cdp.chamar("Page.navigate", url=f"{base}?c={time.time_ns()}#/demonstracao?perfil={perfil}&retorno={urllib.parse.quote(retorno, safe='')}")
    time.sleep(3)
    # A tela de demonstração foca o campo do identificador (ou o botão, no
    # administrador) quando o perfil vem na URL: digita e confirma com Enter.
    if ident:
        cdp.chamar("Input.insertText", text=ident)
        time.sleep(0.3)
    for tipo in ("rawKeyDown", "keyUp"):
        cdp.chamar("Input.dispatchKeyEvent", type=tipo, key="Enter", code="Enter", windowsVirtualKeyCode=13)
    time.sleep(4)


def main():
    import urllib.parse  # noqa: F401
    ap = argparse.ArgumentParser()
    ap.add_argument("--chrome", required=True)
    ap.add_argument("--url", default="http://127.0.0.1:8000/")
    ap.add_argument("--saida", default="capturas")
    args = ap.parse_args()
    os.makedirs(args.saida, exist_ok=True)

    perfil_dir = tempfile.mkdtemp(prefix="capturas-chrome-")
    proc = subprocess.Popen([args.chrome, "--headless=new", "--remote-debugging-port=9334", f"--user-data-dir={perfil_dir}",
                             "--no-first-run", "about:blank"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        for _ in range(50):
            try:
                alvo = next(a for a in json.load(urllib.request.urlopen("http://127.0.0.1:9334/json")) if a["type"] == "page")
                break
            except Exception:
                time.sleep(0.2)
        cdp = Cdp(alvo["webSocketDebuggerUrl"])
        cdp.chamar("Page.enable")
        resultado = []
        for largura, altura in LARGURAS:
            cdp.chamar("Emulation.setDeviceMetricsOverride", width=largura, height=altura, deviceScaleFactor=1, mobile=largura < 600)
            for nome, rota, perfil, ident in ROTAS:
                if perfil:
                    entrar(cdp, args.url, perfil, ident, rota)
                else:
                    cdp.chamar("Page.navigate", url=f"{args.url}?c={time.time_ns()}#{rota}")
                    time.sleep(3)
                medidas = cdp.js("({rolagem: document.documentElement.scrollWidth, largura: document.documentElement.clientWidth, rota: location.hash})")
                png = base64.b64decode(cdp.chamar("Page.captureScreenshot", format="png")["data"])
                arquivo = os.path.join(args.saida, f"{largura}-{nome}.png")
                with open(arquivo, "wb") as f:
                    f.write(png)
                linha = {"largura": largura, "tela": nome, **medidas, "arquivo": arquivo}
                resultado.append(linha)
                print(json.dumps(linha, ensure_ascii=False), flush=True)
        with open(os.path.join(args.saida, "resultado.json"), "w", encoding="utf-8") as f:
            json.dump(resultado, f, ensure_ascii=False, indent=2)
    finally:
        proc.terminate()
        proc.wait(timeout=10)


if __name__ == "__main__":
    import urllib.parse
    main()
