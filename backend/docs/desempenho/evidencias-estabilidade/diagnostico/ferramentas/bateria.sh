#!/bin/bash
# Uso: bateria.sh <subpasta> <N>  -> aquecimento, principal e N rodadas da complementar (aplicação já em pé)
SUB=$1
N=${2:-10}
S="/c/Users/Wilgner/AppData/Local/Temp/claude/5ef27b68719b0d0b/a53f5d23-51aa-4b3f-bc56-a0e0c6295816/scratchpad"
bash "$S/rodar-fase.sh" aquecimento "$SUB-00-aquecimento" "estabilidade/$SUB" > /dev/null
bash "$S/rodar-fase.sh" principal "$SUB-00-principal" "estabilidade/$SUB" > /dev/null
for i in $(seq 1 "$N"); do
  bash "$S/rodar-fase.sh" complementar "$SUB-complementar-$(printf '%02d' "$i")" "estabilidade/$SUB" | grep -v "tcp:8080"
done
echo "BATERIA $SUB CONCLUIDA"
