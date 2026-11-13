"""Mede a primeira tela útil do catálogo com o Chrome headless (CDP).

Perfis de TESTES_E_IMPLEMENTACAO.md:
  computador: 10 Mbps, RTT 50 ms, sem desaceleração de CPU
  entrada:     4 Mbps, RTT 150 ms, CPU 4x mais lenta (simulação de laboratório)

Para cada perfil, faz N cargas frias (cache e armazenamento limpos) e N
quentes (cache do navegador preservado). A marca `primeira-tela-util` é
registrada pelo app depois do quadro que apresenta a lista do catálogo.

Uso (com o Caddy servindo o build em http://127.0.0.1:8000):
  python -m pip install websocket-client==1.8.0
  python tool/medir_desempenho.py --chrome "C:/Program Files/Google/Chrome/Application/chrome.exe"
"""

import argparse
import json
import os
import statistics
import subprocess
import tempfile
import time
import urllib.request

import websocket

PERFIS = {
    "computador": {"down": 10_000_000 / 8, "up": 5_000_000 / 8, "rtt": 50, "cpu": 1},
    "entrada": {"down": 4_000_000 / 8, "up": 2_000_000 / 8, "rtt": 150, "cpu": 4},
}


class Cdp:
    def __init__(self, url_ws):
        self.ws = websocket.create_connection(url_ws, timeout=60)
        self.seq = 0

    def chamar(self, metodo, **params):
        self.seq += 1
        self.ws.send(json.dumps({"id": self.seq, "method": metodo, "params": params}))
        while True:
            msg = json.loads(self.ws.recv())
            if msg.get("id") == self.seq:
                if "error" in msg:
                    raise RuntimeError(f"{metodo}: {msg['error']}")
                return msg.get("result", {})


def avaliar(cdp, expressao):
    r = cdp.chamar("Runtime.evaluate", expression=expressao, returnByValue=True)
    return r.get("result", {}).get("value")


def medir(cdp, url, perfil, fria, prazo=60):
    p = PERFIS[perfil]
    cdp.chamar("Network.emulateNetworkConditions", offline=False, latency=p["rtt"],
               downloadThroughput=p["down"], uploadThroughput=p["up"])
    cdp.chamar("Emulation.setCPUThrottlingRate", rate=p["cpu"])
    if fria:
        cdp.chamar("Network.clearBrowserCache")
        cdp.chamar("Storage.clearDataForOrigin", origin="http://127.0.0.1:8000", storageTypes="all")
    cdp.chamar("Page.navigate", url=f"{url}?r={time.time_ns()}#/pontos")
    inicio = time.time()
    while time.time() - inicio < prazo:
        valor = avaliar(cdp, """(() => {
            const m = performance.getEntriesByName('primeira-tela-util')[0];
            if (!m) return null;
            const r = performance.getEntriesByType('resource');
            const n = performance.getEntriesByType('navigation')[0];
            const api = r.filter(e => e.name.includes('/api/'));
            return {tela: m.startTime,
                    bytes: r.reduce((s, e) => s + e.transferSize, 0) + (n ? n.transferSize : 0),
                    requisicoes: r.length + 1,
                    api: api.map(e => e.name.replace(location.origin, ''))};
        })()""")
        if valor:
            return valor
        time.sleep(0.2)
    raise TimeoutError("primeira tela útil não registrada")


def percentil(valores, p):
    ordenados = sorted(valores)
    k = (len(ordenados) - 1) * p
    i = int(k)
    return ordenados[i] if i + 1 >= len(ordenados) else ordenados[i] + (ordenados[i + 1] - ordenados[i]) * (k - i)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--chrome", required=True)
    ap.add_argument("--url", default="http://127.0.0.1:8000/")
    ap.add_argument("--repeticoes", type=int, default=15)
    args = ap.parse_args()

    perfil_dir = tempfile.mkdtemp(prefix="medicao-chrome-")
    proc = subprocess.Popen([args.chrome, "--headless=new", "--remote-debugging-port=9333",
                             f"--user-data-dir={perfil_dir}", "--window-size=1366,768",
                             "--no-first-run", "--no-default-browser-check", "about:blank"],
                            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    try:
        for _ in range(50):
            try:
                alvos = json.load(urllib.request.urlopen("http://127.0.0.1:9333/json"))
                pagina = next(a for a in alvos if a["type"] == "page")
                break
            except Exception:
                time.sleep(0.2)
        cdp = Cdp(pagina["webSocketDebuggerUrl"])
        cdp.chamar("Page.enable")
        cdp.chamar("Network.enable")
        versao = json.load(urllib.request.urlopen("http://127.0.0.1:9333/json/version"))["Browser"]
        resultado = {"navegador": versao, "repeticoes": args.repeticoes, "perfis": {}}
        for perfil in PERFIS:
            resultado["perfis"][perfil] = {}
            for fria in (True, False):
                if not fria:
                    medir(cdp, args.url, perfil, fria=False)  # aquece o cache neste perfil
                amostras = [medir(cdp, args.url, perfil, fria) for _ in range(args.repeticoes)]
                tempos = [a["tela"] for a in amostras]
                chave = "fria" if fria else "quente"
                resultado["perfis"][perfil][chave] = {
                    "p50_ms": round(percentil(tempos, 0.5)),
                    "p75_ms": round(percentil(tempos, 0.75)),
                    "p95_ms": round(percentil(tempos, 0.95)),
                    "min_ms": round(min(tempos)),
                    "max_ms": round(max(tempos)),
                    "bytes_transferidos_mediana": int(statistics.median(a["bytes"] for a in amostras)),
                    "requisicoes": amostras[0]["requisicoes"],
                    "chamadas_api": amostras[0]["api"],
                }
                print(perfil, chave, json.dumps(resultado["perfis"][perfil][chave], ensure_ascii=False), flush=True)
        print(json.dumps(resultado, ensure_ascii=False, indent=2))
    finally:
        proc.terminate()
        proc.wait(timeout=10)


if __name__ == "__main__":
    main()
