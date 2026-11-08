#!/bin/bash
# Teste B: k6 (via Docker) em paralelo com a sonda do host. Repete até N rodadas.
N=${1:-5}
S="/c/Users/Wilgner/AppData/Local/Temp/claude/5ef27b68719b0d0b/a53f5d23-51aa-4b3f-bc56-a0e0c6295816/scratchpad"
mkdir -p "$S/estabilidade/testeB"
for i in $(seq 1 "$N"); do
  R=$(printf '%02d' "$i")
  python -X utf8 "$S/sonda.py" 75 "$S/estabilidade/testeB/sonda-$R.txt" > "$S/estabilidade/testeB/sonda-$R.resumo" 2>&1 &
  PID=$!
  sleep 2
  bash "$S/rodar-fase.sh" complementar "testeB-k6-$R" estabilidade/testeB
  wait $PID
  echo "   $(cat "$S/estabilidade/testeB/sonda-$R.resumo")"
done
echo "TESTE B CONCLUIDO"
