"""Para cada rodada do k6 (linhas dos _indice.txt), calcula o maior silêncio do gvisor/forwarder do Docker Desktop
dentro da janela da rodada e o confronta com o resultado (k6=0 aprovada, k6=99 reprovada) e os timeouts do log do k6.

Uso: python analisar-forwarder.py <pasta-docker-logs> <saida.txt> <_indice.txt>...
Os horários dos _indice.txt são locais (UTC-3); os do monitor.log são UTC.
"""
import glob
import json
import re
import sys
from datetime import datetime, timedelta

pasta, saida, indices = sys.argv[1], sys.argv[2], sys.argv[3:]
FMT = "%Y-%m-%dT%H:%M:%S.%f"

eventos = []  # instantes (UTC) de qualquer evento do forwarder
for arq in sorted(glob.glob(pasta + "/monitor.log*")):
    for l in open(arq, encoding="utf-8", errors="replace"):
        m = re.match(r"\[(2026-11-08T[\d:.]+)\d*Z\] (\{.*\})", l)
        if not m or '"component":"gvisor/forwarder"' not in l:
            continue
        eventos.append(datetime.strptime(m.group(1)[:26], FMT))
eventos = sorted(set(eventos))

linhas = ["rodada | k6 | duração | maior silêncio do forwarder na janela (s) | eventos do forwarder"]
for ind in indices:
    for l in open(ind, encoding="utf-8"):
        m = re.match(r"(\S+) k6=(\d+) (\d\d:\d\d:\d\d)-(\d\d:\d\d:\d\d)", l)
        if not m or ("complementar" not in m.group(1) and "k6-" not in m.group(1)):
            continue
        nome, cod, ini, fim = m.groups()
        h = lambda s: datetime.strptime("2026-11-08T" + s + ".000000", FMT) + timedelta(hours=3)  # local -> UTC
        a, b = h(ini), h(fim)
        dentro = [e for e in eventos if a + timedelta(seconds=4) <= e <= b]  # ignora os 4 s do setup()
        if len(dentro) < 2:
            linhas.append(f"{nome} | {'aprovada' if cod == '0' else 'REPROVADA'} | {(b-a).seconds}s | n/a (quase nenhuma conexão nova: keep-alive ilimitado) | {len(dentro)}")
            continue
        silencio = max((y - x).total_seconds() for x, y in zip(dentro, dentro[1:]))
        linhas.append(f"{nome} | {'aprovada' if cod == '0' else 'REPROVADA'} | {(b-a).seconds}s | {silencio:.1f} | {len(dentro)}")

open(saida, "w", encoding="utf-8").write("\n".join(linhas) + "\n")
print("\n".join(linhas))
