#!/bin/bash
# Uso: rodar-fase.sh <fase> <rotulo> [pasta-saida] [k6-extra...]
# Roda UMA fase do roteiro k6 oficial (inalterado) e guarda: log completo, resumo JSON e retrato das conexões TCP.
FASE=$1
ROTULO=$2
SAIDA=${3:-estabilidade}
SCRATCH="/c/Users/Wilgner/AppData/Local/Temp/claude/5ef27b68719b0d0b/a53f5d23-51aa-4b3f-bc56-a0e0c6295816/scratchpad"
BASE="$SCRATCH/$SAIDA"
mkdir -p "$BASE"
cd /c/Stack/Projetos/sistema-coleta-seletiva || exit 1
VOL=$(cd "$BASE" && pwd -W)

contar_tcp() {
  netstat -ano -p tcp | tr -d '\r' | grep -E ':8080 ' | awk '{print $4}' | sort | uniq -c | tr '\n' ' '
}

ANTES=$(contar_tcp)
INI=$(date +%H:%M:%S)
MSYS_NO_PATHCONV=1 docker run --rm -i -e FASE="$FASE" -v "$VOL:/saida" grafana/k6:1.8.2 \
  run --summary-export="/saida/$ROTULO.json" - < docs/desempenho/k6-consultas.js > "$BASE/$ROTULO.log" 2>&1
COD=$?
FIM=$(date +%H:%M:%S)
DEPOIS=$(contar_tcp)
TIMEOUTS=$(grep -c "i/o timeout" "$BASE/$ROTULO.log")
python -X utf8 - "$BASE/$ROTULO.json" "$ROTULO" "$COD" "$TIMEOUTS" "$INI" "$FIM" <<'EOF'
import json, sys
arq, rot, cod, to, ini, fim = sys.argv[1:7]
try:
    m = json.load(open(arq, encoding="utf-8"))["metrics"]
    r = m["http_reqs"]; f = m["http_req_failed"]; c = m["checks"]
    p95 = m["http_req_duration{etapa:medicao}"]["p(95)"]
    print(f"{rot}: k6={cod} {ini}-{fim} reqs={r['count']} ({r['rate']:.0f}/s) http_falhas={f['passes']} checks_falhos={c['fails']} p95={p95:.2f}ms timeouts_no_log={to}")
except Exception as e:
    print(f"{rot}: k6={cod} {ini}-{fim} sem resumo ({e}) timeouts_no_log={to}")
EOF
echo "   tcp:8080 antes: $ANTES | depois: $DEPOIS"
echo "$ROTULO k6=$COD $INI-$FIM tcp_antes=[$ANTES] tcp_depois=[$DEPOIS]" >> "$BASE/_indice.txt"
