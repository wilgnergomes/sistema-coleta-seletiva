#!/bin/bash
# Validação do contorno (Issue #58): aquecimento, principal e N rodadas da fase complementar (10 VUs, 30 s),
# na aplicação subida com SERVER_TOMCAT_MAX_KEEP_ALIVE_REQUESTS=-1. PARA na primeira rodada reprovada.
N=${1:-15}
SUB=validacao-15-rodadas
S="/c/Users/Wilgner/AppData/Local/Temp/claude/5ef27b68719b0d0b/a53f5d23-51aa-4b3f-bc56-a0e0c6295816/scratchpad"
bash "$S/rodar-fase.sh" aquecimento "validacao-00-aquecimento" "estabilidade/$SUB" > /dev/null
bash "$S/rodar-fase.sh" principal "validacao-00-principal" "estabilidade/$SUB" | grep -v "tcp:8080"
for i in $(seq 1 "$N"); do
  R=$(printf '%02d' "$i")
  bash "$S/rodar-fase.sh" complementar "validacao-complementar-$R" "estabilidade/$SUB" | grep -v "tcp:8080" | tee /tmp/ultima-rodada.txt
  if ! grep -q "k6=0 " /tmp/ultima-rodada.txt; then
    echo "PARADA: a rodada $R não foi aprovada."
    exit 1
  fi
done
echo "VALIDACAO CONCLUIDA: $N rodadas aprovadas"
