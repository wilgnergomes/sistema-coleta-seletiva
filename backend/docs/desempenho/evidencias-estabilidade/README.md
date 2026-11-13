# Evidências: instabilidade da fase complementar do k6 (Issue #58)

Reúne o que foi medido para diagnosticar e contornar o `dial: i/o timeout` da fase `complementar` (10 VUs, 30 s). A análise, as conclusões e o que **não** foi comprovado estão na seção 3.1 do [`relatorio-rnf04.md`](../relatorio-rnf04.md). Esta pasta guarda apenas as saídas brutas e as ferramentas.

Data: 2026-11-08 · Banco: `coleta_desempenho` (V1, V2 e V3 aplicadas do zero pelo Flyway, dataset corrigido) · k6 `grafana/k6:1.8.2` · Docker Desktop 29.8.1 (backend WSL2, kernel 6.18.54.1-microsoft-standard-WSL2) · Tomcat 11.0.24 (Spring Boot 4.1.1).

| Pasta ou arquivo | Conteúdo |
|---|---|
| [`diagnostico/reproducao-k6-docker/`](diagnostico/reproducao-k6-docker) | Reprodução com o procedimento oficial **sem** o contorno: aquecimento, principal e 8 rodadas da complementar. 6 reprovadas, todas só por `dial: i/o timeout`. Cada rodada tem `.log` (saída completa do k6, com o instante de cada timeout), `.json` (resumo) e o retrato de conexões TCP da porta 8080 em `_indice.txt` |
| [`diagnostico/teste-a-host-sem-docker/`](diagnostico/teste-a-host-sem-docker) | **Teste A:** o mesmo tráfego (10 threads, 30 s) feito direto do host, sem Docker. 8 de 8 rodadas sem erro |
| [`diagnostico/teste-b-sonda-no-host/`](diagnostico/teste-b-sonda-no-host) | **Teste B:** k6 via Docker em paralelo com uma sonda que abre uma conexão nova no host a cada 250 ms (`sonda-NN.txt`). 1 rodada reprovada e a sonda sem nenhum erro |
| [`diagnostico/docker-desktop-monitor-teste-b-rodada-4.log`](diagnostico/docker-desktop-monitor-teste-b-rodada-4.log) | Trecho cru (21:29:30 a 21:30:06 UTC) do `monitor.log` do Docker Desktop durante a rodada reprovada do Teste B: o silêncio do `gvisor/forwarder` entre 21:29:35,659 e 21:30:04,835 |
| [`diagnostico/analise-forwarder-por-rodada.txt`](diagnostico/analise-forwarder-por-rodada.txt) | Maior silêncio do forwarder em cada rodada (reprovada ou aprovada), calculado com os `monitor.log` rotativos do Docker |
| [`diagnostico/teste-c-keepalive-ilimitado/`](diagnostico/teste-c-keepalive-ilimitado) | **Teste C:** aplicação com `SERVER_TOMCAT_MAX_KEEP_ALIVE_REQUESTS=-1`. 10 de 10 aprovadas |
| [`validacao-15-rodadas/`](validacao-15-rodadas) | **Validação do contorno** pelo procedimento do [README](../README.md): aquecimento, principal e 15 rodadas da complementar, todas registradas. 15 de 15 aprovadas |
| [`diagnostico/ferramentas/`](diagnostico/ferramentas) | Scripts usados: `rodar-fase.sh` (uma fase do k6 com log, resumo e conexões TCP), `bateria-reproducao.sh`, `bateria.sh` e `bateria-validacao.sh`, `teste-b.sh`, `carga.py` e `sonda.py` (tráfego e sonda do host) e `analisar-forwarder.py` |

## Observações para quem for ler ou repetir

- Os scripts de `ferramentas/` foram escritos para o Git Bash do Windows e trazem o caminho da pasta temporária desta sessão; ajuste a variável `SCRATCH` ou `S` no topo de cada um. O roteiro `docs/desempenho/k6-consultas.js` **não foi alterado**.
- Os horários de `_indice.txt` são locais (UTC-3); os do `monitor.log` do Docker e dos `.log` do k6 são UTC.
- O Docker Desktop rotaciona o `monitor.log` a cada ~1 MB e apaga os antigos. Por isso o trecho cru foi copiado para cá e a análise por rodada foi guardada em texto.
- Os arquivos `repro-00-aquecimento.*` não têm resumo de p95 porque a fase de aquecimento não é medida.
- O `monitor.log` do Docker é um log interno do produto: o formato pode mudar entre versões. O que ele mostra aqui é só o comportamento do `gvisor/forwarder` desta instalação, e o **mecanismo interno do Docker Desktop continua não comprovado**.
- Se o Git converter os scripts `.sh` para CRLF ao baixar o repositório no Windows, o Bash os recusa (`\r: command not found`); remova o retorno de carro com `sed -i 's/\r$//' arquivo.sh`.
