#!/bin/bash
# Reproduz o procedimento oficial: aquecimento -> principal -> complementar repetida N vezes, sobre a aplicação já em pé.
N=${1:-8}
SCRATCH="/c/Users/Wilgner/AppData/Local/Temp/claude/5ef27b68719b0d0b/a53f5d23-51aa-4b3f-bc56-a0e0c6295816/scratchpad"
RODAR="$SCRATCH/rodar-fase.sh"
bash "$RODAR" aquecimento repro-00-aquecimento estabilidade/repro
bash "$RODAR" principal repro-00-principal estabilidade/repro
for i in $(seq 1 "$N"); do
  bash "$RODAR" complementar "repro-complementar-$(printf '%02d' "$i")" estabilidade/repro
done
echo "BATERIA CONCLUIDA"
