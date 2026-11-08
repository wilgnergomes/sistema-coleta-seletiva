"""Sonda no host: a cada 250 ms abre uma conexão NOVA em 127.0.0.1:8080 e faz um GET leve.

Uso: python sonda.py <segundos> <arquivo-saida>
Grava uma linha por tentativa: instante relativo (s), tempo de connect (ms), tempo da resposta (ms) ou ERRO.
Mostra no fim quantas passaram de 200 ms e o pior caso. Serve para provar se o Windows/Tomcat continuam
aceitando conexões novas enquanto outro cliente (o k6, via Docker) está travado.
"""
import http.client
import sys
import time

segundos = int(sys.argv[1])
saida = sys.argv[2]
inicio = time.time()
n = lentas = erros = 0
pior_connect = pior_resp = 0.0
with open(saida, "w", encoding="utf-8") as f:
    while time.time() - inicio < segundos:
        t_ini = time.time()
        rel = t_ini - inicio
        try:
            c = http.client.HTTPConnection("127.0.0.1", 8080, timeout=5)
            t0 = time.perf_counter()
            c.connect()
            cms = (time.perf_counter() - t0) * 1000
            t0 = time.perf_counter()
            c.request("GET", "/resumo-gerencial", headers={"X-Perfil": "ADMINISTRADOR", "X-Usuario-Id": "1"})
            r = c.getresponse()
            r.read()
            rms = (time.perf_counter() - t0) * 1000
            c.close()
            f.write(f"{rel:.2f} connect={cms:.1f} resp={rms:.1f} status={r.status}\n")
            pior_connect = max(pior_connect, cms)
            pior_resp = max(pior_resp, rms)
            if cms > 200 or rms > 200:
                lentas += 1
        except OSError as e:
            erros += 1
            f.write(f"{rel:.2f} ERRO {type(e).__name__}: {e}\n")
        n += 1
        time.sleep(max(0, 0.25 - (time.time() - t_ini)))
print(f"sonda: tentativas={n} erros={erros} acima_de_200ms={lentas} pior_connect={pior_connect:.1f}ms pior_resposta={pior_resp:.1f}ms")
