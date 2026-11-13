import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/api/falhas.dart';
import '../../../core/api/pagina.dart';
import '../../../core/dominio/datas.dart';
import '../../../core/estado/carga.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../catalogo/data/modelos.dart';
import '../../catalogo/data/repositorio_catalogo.dart';
import '../../catalogo/ui/componentes_catalogo.dart';
import '../data/solicitacoes.dart';

/// Limites da derivação dos pontos recentes: uma página de solicitações e
/// poucas leituras de ponto (com cache), nunca a paginação inteira.
const tamanhoPaginaRecentes = 20;
const maximoConsultasPonto = 4;

/// Escolhe até [maximo] pontos usados recentemente, sem repetição, na ordem
/// das solicitações (mais recentes primeiro). Pontos indisponíveis (404) ou
/// sem recolhimento domiciliar ativo ficam de fora, e o município do
/// endereço selecionado, quando houver, filtra o resultado.
Future<List<PontoColeta>> pontosRecentes(
  List<SolicitacaoResumo> solicitacoes,
  Future<PontoColeta> Function(int id) lerPonto, {
  String? municipio,
  int maximo = 2,
  int maximoConsultas = maximoConsultasPonto,
}) async {
  final ids = <int>[];
  for (final s in solicitacoes) {
    if (!ids.contains(s.pontoId)) ids.add(s.pontoId);
  }
  final escolhidos = <PontoColeta>[];
  var consultas = 0;
  for (final id in ids) {
    if (escolhidos.length >= maximo || consultas >= maximoConsultas) break;
    consultas++;
    try {
      final p = await lerPonto(id);
      if (!p.aceitaSolicitacao) continue;
      if (municipio != null && p.endereco.municipio.toLowerCase() != municipio.toLowerCase()) continue;
      escolhidos.add(p);
    } on ErroHttp catch (e) {
      if (!e.naoEncontrado) rethrow;
    }
  }
  return escolhidos;
}

/// Seções do início do morador: pontos usados recentemente e
/// solicitações recentes, a partir de uma única página da API.
class SecoesRecentesMorador extends StatefulWidget {
  const SecoesRecentesMorador({
    super.key,
    required this.municipio,
    required this.solicitacoes,
    required this.catalogo,
    required this.fuso,
    required this.aoAbrirPonto,
    required this.aoAbrirSolicitacao,
    required this.aoVerTodas,
  });

  final String? municipio;
  final RepositorioSolicitacoes solicitacoes;
  final RepositorioCatalogo catalogo;
  final FusoNegocio fuso;
  final ValueChanged<int> aoAbrirPonto;
  final ValueChanged<int> aoAbrirSolicitacao;
  final VoidCallback aoVerTodas;

  @override
  State<SecoesRecentesMorador> createState() => _SecoesRecentesMoradorState();
}

class _SecoesRecentesMoradorState extends State<SecoesRecentesMorador> {
  final _pagina = Carga<Pagina<SolicitacaoResumo>>();
  List<PontoColeta> _pontos = const [];
  int _geracaoPontos = 0;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  @override
  void didUpdateWidget(SecoesRecentesMorador antigo) {
    super.didUpdateWidget(antigo);
    if (antigo.municipio != widget.municipio) _derivarPontos();
  }

  Future<void> _carregar() async {
    await _pagina.executar(() async {
      final r = await Future.wait<Object?>([
        widget.solicitacoes.listar(const FiltroSolicitacoes(), tamanho: tamanhoPaginaRecentes),
        widget.fuso.garantir(),
      ]);
      return r.first as Pagina<SolicitacaoResumo>;
    }, _atualizar);
    await _derivarPontos();
  }

  void _atualizar() {
    if (mounted) setState(() {});
  }

  Future<void> _derivarPontos() async {
    final pagina = _pagina.dados;
    if (pagina == null) return;
    final geracao = ++_geracaoPontos;
    try {
      final pontos = await pontosRecentes(pagina.conteudo, widget.catalogo.ponto, municipio: widget.municipio);
      if (geracao == _geracaoPontos) {
        _pontos = pontos;
        _atualizar();
      }
    } on FalhaApi {
      // A seção é complementar: em falha, simplesmente não aparece.
    }
  }

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final pagina = _pagina.dados;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (_pontos.isNotEmpty) ...[
          const TituloSecao('Pontos usados recentemente'),
          GradeResponsiva(
            larguraMinima: 280,
            filhos: [
              for (final p in _pontos)
                CartaoPonto(key: ValueKey('recente-${p.id}'), ponto: p, aoAbrir: () => widget.aoAbrirPonto(p.id)),
            ],
          ),
        ],
        TituloSecao(
          'Solicitações recentes',
          acao: pagina != null && pagina.totalElementos > 0
              ? TextButton(onPressed: widget.aoVerTodas, child: const Text('Ver todas'))
              : null,
        ),
        if (pagina == null && _pagina.falha != null)
          EstadoErro(mensagem: _pagina.falha!.mensagem, aoTentarNovamente: _carregar)
        else if (pagina == null)
          const EstadoCarregando(rotulo: 'Carregando solicitações…')
        else if (pagina.conteudo.isEmpty)
          const EstadoVazio(
            icone: Icons.receipt_long_outlined,
            titulo: 'Você ainda não tem solicitações',
            mensagem: 'Escolha um ponto com recolhimento domiciliar para fazer o primeiro pedido.',
          )
        else
          for (final s in pagina.conteudo.take(3))
            Padding(
              padding: const EdgeInsets.only(bottom: Espaco.xs),
              child: Card(
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.xxs),
                  title: Text(s.protocolo, style: texto.titleSmall),
                  subtitle: Text(
                    s.agendadaPara == null
                        ? 'Criada em ${widget.fuso.data(s.criadaEm)}'
                        : 'Agendada para ${widget.fuso.dataHora(s.agendadaPara!)}',
                  ),
                  trailing: SeloSituacao(situacao: s.situacao),
                  onTap: () => widget.aoAbrirSolicitacao(s.id),
                ),
              ),
            ),
      ],
    );
  }
}
