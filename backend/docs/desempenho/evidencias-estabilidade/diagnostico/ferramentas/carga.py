"""Gerador de carga do lado do host (SEM Docker), com o mesmo tráfego da fase complementar do k6.

Uso: python carga.py <segundos> <threads> [host] [porta]
10 threads, conexão persistente (reaberta quando o Tomcat fecha), três consultas em rodízio (RF06, RF09, RF15).
Mede o tempo de cada connect() e a vazão por segundo; qualquer connect/resposta acima de 1 s é registrado.
"""
import http.client
import socket
import sys
import threading
import time

segundos = int(sys.argv[1])
n_threads = int(sys.argv[2])
host = sys.argv[3] if len(sys.argv) > 3 else "127.0.0.1"
porta = int(sys.argv[4]) if len(sys.argv) > 4 else 8080

ADMIN = {"X-Perfil": "ADMINISTRADOR", "X-Usuario-Id": "1"}
CONSULTAS = [
    ("/pontos-coleta?municipio=Lagarto&size=20", {}),
    ("/solicitacoes?page=3&size=100", ADMIN),
    ("/resumo-gerencial", ADMIN),
]

fim = time.time() + segundos
trava = threading.Lock()
por_segundo = {}
conexoes = []   # tempos de connect (ms)
lentos = []     # (instante relativo, o que, ms)
erros = []
inicio = time.time()


def trabalhador(idx):
    conn = None
    n = idx
    while time.time() < fim:
        try:
            if conn is None or conn.sock is None:
                conn = http.client.HTTPConnection(host, porta, timeout=35)
                t0 = time.perf_counter()
                conn.connect()
                ms = (time.perf_counter() - t0) * 1000
                with trava:
                    conexoes.append(ms)
                    if ms > 1000:
                        lentos.append((round(time.time() - inicio, 1), "connect", round(ms)))
            caminho, cab = CONSULTAS[n % 3]
            n += 1
            t0 = time.perf_counter()
            conn.request("GET", caminho, headers=cab)
            resp = conn.getresponse()
            resp.read()
            ms = (time.perf_counter() - t0) * 1000
            seg = int(time.time() - inicio)
            with trava:
                por_segundo[seg] = por_segundo.get(seg, 0) + 1
                if ms > 1000:
                    lentos.append((round(time.time() - inicio, 1), "resposta", round(ms)))
            if resp.status != 200:
                with trava:
                    erros.append(f"status {resp.status} em {caminho}")
        except (OSError, http.client.HTTPException) as e:
            with trava:
                erros.append(f"{type(e).__name__}: {e}")
            try:
                conn.close()
            except Exception:
                pass
            conn = None


ts = [threading.Thread(target=trabalhador, args=(i,)) for i in range(n_threads)]
for t in ts:
    t.start()
for t in ts:
    t.join()

total = sum(por_segundo.values())
dur = time.time() - inicio
vazios = [s for s in range(segundos) if por_segundo.get(s, 0) < 50]
print(f"duracao={dur:.1f}s requisicoes={total} ({total/dur:.0f}/s) conexoes_abertas={len(conexoes)} "
      f"connect_max={max(conexoes) if conexoes else 0:.1f}ms erros={len(erros)} "
      f"segundos_quase_parados={vazios} lentos(>1s)={lentos[:6]}")
if erros:
    print("primeiros erros:", erros[:3])
