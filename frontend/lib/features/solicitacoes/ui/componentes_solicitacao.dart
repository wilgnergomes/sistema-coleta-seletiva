import 'package:flutter/services.dart';
import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tema.dart';
import '../../../app/tema/tokens.dart';
import '../../../ui/compartilhado/superficie.dart';
import '../../../core/dominio/datas.dart';
import '../../../core/dominio/enums.dart';
import '../../../ui/compartilhado/campos.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/layout.dart';
import '../../../ui/compartilhado/material_visual.dart';
import '../data/solicitacoes.dart';

/// Cartão da lista: protocolo, situação, datas, ponto e endereço resumido.
class CartaoSolicitacao extends StatelessWidget {
  const CartaoSolicitacao({
    super.key,
    required this.solicitacao,
    required this.nomePonto,
    required this.fuso,
    required this.aoAbrir,
    this.selecionada = false,
    this.mostrarMorador = false,
  });

  final SolicitacaoResumo solicitacao;
  final String nomePonto;
  final FusoNegocio fuso;
  final VoidCallback aoAbrir;
  final bool selecionada;
  final bool mostrarMorador;

  @override
  Widget build(BuildContext context) {
    final s = solicitacao;
    final texto = Theme.of(context).textTheme;
    final visual = context.coresSituacao.solicitacao(s.situacao);
    return CartaoInterativo(
      altura: 168,
      selecionado: selecionada,
      aoTocar: aoAbrir,
      rotuloSemantico: 'Solicitação ${s.protocolo}, ${s.situacao.rotulo}',
      preenchimento: EdgeInsets.zero,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Faixa lateral com a cor da situação.
          Container(
            width: 4,
            decoration: BoxDecoration(
              color: visual.cor,
              borderRadius: const BorderRadius.horizontal(left: Radius.circular(Raio.cartao)),
            ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(Espaco.m),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          s.protocolo,
                          style: texto.titleMedium,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: Espaco.xs),
                      SeloSituacao(situacao: s.situacao),
                    ],
                  ),
                  const SizedBox(height: Espaco.xs),
                  Text('Criada em ${fuso.dataHora(s.criadaEm)}', style: texto.bodyMedium, maxLines: 1),
                  Text(
                    s.agendadaPara == null ? 'Ainda não agendada' : 'Agendada para ${fuso.dataHora(s.agendadaPara!)}',
                    style: texto.bodyMedium,
                    maxLines: 1,
                  ),
                  const Spacer(),
                  Text(
                    mostrarMorador ? '$nomePonto · Morador #${s.moradorId}' : nomePonto,
                    style: texto.bodySmall,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(s.enderecoCopia, style: texto.bodySmall, maxLines: 1, overflow: TextOverflow.ellipsis),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// Filtro rápido de situação, em chips de seleção única.
class FiltroSituacao extends StatelessWidget {
  const FiltroSituacao({super.key, required this.selecionada, required this.aoMudar});

  final SituacaoSolicitacao? selecionada;
  final ValueChanged<SituacaoSolicitacao?> aoMudar;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      container: true,
      label: 'Filtrar por situação',
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            for (final s in <SituacaoSolicitacao?>[null, ...SituacaoSolicitacao.conhecidas]) ...[
              ChoiceChip(label: Text(s?.rotulo ?? 'Todas'), selected: s == selecionada, onSelected: (_) => aoMudar(s)),
              const SizedBox(width: Espaco.xs),
            ],
          ],
        ),
      ),
    );
  }
}

/// Período de criação (`de` e `ate`), com validação de ordem.
Future<(DataCivil?, DataCivil?)?> escolherPeriodo(
  BuildContext context, {
  DataCivil? de,
  DataCivil? ate,
  String titulo = 'Período de criação',
}) {
  return showDialog<(DataCivil?, DataCivil?)>(
    context: context,
    builder: (context) => _DialogoPeriodo(de: de, ate: ate, titulo: titulo),
  );
}

class _DialogoPeriodo extends StatefulWidget {
  const _DialogoPeriodo({required this.de, required this.ate, required this.titulo});

  final DataCivil? de;
  final DataCivil? ate;
  final String titulo;

  @override
  State<_DialogoPeriodo> createState() => _DialogoPeriodoState();
}

class _DialogoPeriodoState extends State<_DialogoPeriodo> {
  late DataCivil? _de = widget.de;
  late DataCivil? _ate = widget.ate;

  Future<void> _escolher(bool inicio) async {
    final atual = (inicio ? _de : _ate)?.comoDateTime ?? DateTime.now();
    final escolhida = await showDatePicker(
      context: context,
      initialDate: atual,
      firstDate: DateTime(2020),
      lastDate: DateTime(2100),
      helpText: inicio ? 'Data inicial' : 'Data final',
    );
    if (escolhida == null) return;
    setState(() => inicio ? _de = DataCivil.deDateTime(escolhida) : _ate = DataCivil.deDateTime(escolhida));
  }

  @override
  Widget build(BuildContext context) {
    final invertido = _de != null && _ate != null && _ate!.compareTo(_de!) < 0;
    Widget campo(String rotulo, DataCivil? valor, bool inicio) => Padding(
      padding: const EdgeInsets.only(bottom: Espaco.s),
      child: OutlinedButton.icon(
        onPressed: () => _escolher(inicio),
        icon: const Icon(Icons.calendar_today_outlined),
        label: Text('$rotulo: ${valor?.exibicao ?? 'sem limite'}'),
      ),
    );
    return AlertDialog(
      title: Text(widget.titulo),
      content: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: Largura.dialogo),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('As datas são dias inteiros no fuso de negócio.', style: Theme.of(context).textTheme.bodySmall),
            const SizedBox(height: Espaco.m),
            campo('De', _de, true),
            campo('Até', _ate, false),
            if (invertido)
              Text(
                'A data final deve ser igual ou posterior à inicial.',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(color: Cores.erro),
              ),
          ],
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop((null, null)), child: const Text('Limpar período')),
        FilledButton(
          onPressed: invertido ? null : () => Navigator.of(context).pop((_de, _ate)),
          child: const Text('Aplicar período'),
        ),
      ],
    );
  }
}

String rotuloPeriodo(DataCivil? de, DataCivil? ate) {
  if (de == null && ate == null) return 'Todo o período';
  if (de != null && ate != null) return '${de.exibicao} a ${ate.exibicao}';
  if (de != null) return 'A partir de ${de.exibicao}';
  return 'Até ${ate!.exibicao}';
}

/// Diálogo de motivo obrigatório (recusa, cancelamento), que identifica o
/// registro e devolve o texto. [inicial] preserva o rascunho anterior.
Future<String?> pedirMotivo(
  BuildContext context, {
  required String titulo,
  required String descricao,
  required String rotuloAcao,
  String? inicial,
  bool destrutiva = true,
}) {
  return showDialog<String>(
    context: context,
    builder: (context) => _DialogoMotivo(
      titulo: titulo,
      descricao: descricao,
      rotuloAcao: rotuloAcao,
      inicial: inicial,
      destrutiva: destrutiva,
    ),
  );
}

class _DialogoMotivo extends StatefulWidget {
  const _DialogoMotivo({
    required this.titulo,
    required this.descricao,
    required this.rotuloAcao,
    required this.destrutiva,
    this.inicial,
  });

  final String titulo;
  final String descricao;
  final String rotuloAcao;
  final String? inicial;
  final bool destrutiva;

  @override
  State<_DialogoMotivo> createState() => _DialogoMotivoState();
}

class _DialogoMotivoState extends State<_DialogoMotivo> {
  final _form = GlobalKey<FormState>();
  late final _motivo = TextEditingController(text: widget.inicial);

  @override
  void dispose() {
    _motivo.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text(widget.titulo),
      scrollable: true,
      content: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: Largura.dialogo),
        child: Form(
          key: _form,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(widget.descricao),
              const SizedBox(height: Espaco.m),
              CampoTexto(controlador: _motivo, rotulo: 'Motivo', obrigatorio: true, limite: 500, linhas: 3),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Voltar')),
        FilledButton(
          style: widget.destrutiva ? FilledButton.styleFrom(backgroundColor: Cores.erro) : null,
          onPressed: () {
            if (_form.currentState!.validate()) Navigator.of(context).pop(_motivo.text.trim());
          },
          child: Text(widget.rotuloAcao),
        ),
      ],
    );
  }
}

/// Conteúdo comum do detalhe: identificação, endereço copiado, datas,
/// itens com unidade copiada e histórico.
class DetalheSolicitacaoView extends StatelessWidget {
  const DetalheSolicitacaoView({
    super.key,
    required this.detalhe,
    required this.nomePonto,
    required this.fuso,
    this.acoes,
    this.aviso,
    this.chaveHistorico,
    this.mostrarMorador = false,
    this.aoAbrirResponsavel,
  });

  final SolicitacaoDetalhe detalhe;
  final String nomePonto;
  final FusoNegocio fuso;
  final Widget? acoes;
  final Widget? aviso;
  final GlobalKey? chaveHistorico;
  final bool mostrarMorador;

  /// Abre o cadastro do morador ou do operador (administração).
  final void Function(TipoResponsavel tipo, int id)? aoAbrirResponsavel;

  @override
  Widget build(BuildContext context) {
    final d = detalhe;
    final texto = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Cabeçalho com a cor da situação: identifica o pedido e o estado de
        // relance, antes das ações.
        AnimatedContainer(
          duration: duracao(context, 280),
          padding: const EdgeInsets.all(Espaco.m),
          decoration: BoxDecoration(
            color: context.coresSituacao.solicitacao(d.situacao).fundo,
            borderRadius: BorderRadius.circular(Raio.cartao),
            border: Border(left: BorderSide(color: context.coresSituacao.solicitacao(d.situacao).cor, width: 4)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Wrap(
                spacing: Espaco.s,
                runSpacing: Espaco.xs,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Semantics(header: true, child: Text(d.protocolo, style: texto.headlineSmall)),
                  IconButton(
                    tooltip: 'Copiar protocolo',
                    icon: const Icon(Icons.copy_outlined),
                    onPressed: () async {
                      try {
                        await Clipboard.setData(ClipboardData(text: d.protocolo));
                        if (context.mounted) _avisar(context, 'Protocolo copiado.');
                      } on Object {
                        if (context.mounted) {
                          _avisar(context, 'Não foi possível copiar. Selecione o protocolo no texto.');
                        }
                      }
                    },
                  ),
                  SeloSituacao(situacao: d.situacao),
                ],
              ),
              const SizedBox(height: Espaco.xs),
              Text(d.enderecoCopia, style: texto.bodyLarge),
            ],
          ),
        ),
        if (aviso != null) ...[const SizedBox(height: Espaco.m), aviso!],
        if (acoes != null) ...[const SizedBox(height: Espaco.m), acoes!],
        const TituloSecao('Dados da solicitação'),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(Espaco.m),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                CampoLeitura(rotulo: 'Ponto de coleta', valor: nomePonto),
                if (mostrarMorador)
                  _CampoComAcao(
                    rotulo: 'Morador',
                    valor: 'Morador #${d.moradorId}',
                    aoAbrir: aoAbrirResponsavel == null
                        ? null
                        : () => aoAbrirResponsavel!(TipoResponsavel.morador, d.moradorId),
                  ),
                CampoLeitura(rotulo: 'Endereço do recolhimento (cópia da criação)', valor: d.enderecoCopia),
                CampoLeitura(rotulo: 'Criada em', valor: fuso.dataHora(d.criadaEm)),
                CampoLeitura(
                  rotulo: 'Agendada para',
                  valor: d.agendadaPara == null ? 'Ainda não agendada' : fuso.dataHora(d.agendadaPara!),
                ),
                if (d.concluidaEm != null) CampoLeitura(rotulo: 'Concluída em', valor: fuso.dataHora(d.concluidaEm!)),
                CampoLeitura(
                  rotulo: 'Observações',
                  valor: d.observacoes == null || d.observacoes!.isEmpty ? 'Sem observações' : d.observacoes!,
                ),
                Text('Horários no fuso ${fuso.rotulo(d.criadaEm)} (${fuso.nome}).', style: texto.bodySmall),
              ],
            ),
          ),
        ),
        const TituloSecao('Materiais'),
        for (final item in d.itens) ...[_LinhaItem(item: item), const SizedBox(height: Espaco.xs)],
        TituloSecao('Histórico', key: chaveHistorico),
        _LinhaDoTempo(historico: d.historico, fuso: fuso, aoAbrirResponsavel: aoAbrirResponsavel),
      ],
    );
  }

  static void _avisar(BuildContext context, String mensagem) =>
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(mensagem)));
}

class _CampoComAcao extends StatelessWidget {
  const _CampoComAcao({required this.rotulo, required this.valor, this.aoAbrir});

  final String rotulo;
  final String valor;
  final VoidCallback? aoAbrir;

  @override
  Widget build(BuildContext context) {
    if (aoAbrir == null) return CampoLeitura(rotulo: rotulo, valor: valor);
    return Row(
      children: [
        Expanded(
          child: CampoLeitura(rotulo: rotulo, valor: valor),
        ),
        TextButton(
          onPressed: aoAbrir,
          child: Text('Abrir cadastro', semanticsLabel: 'Abrir cadastro de $valor'),
        ),
      ],
    );
  }
}

class _LinhaItem extends StatelessWidget {
  const _LinhaItem({required this.item});

  final ItemSolicitacao item;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final efetiva = item.quantidadeEfetiva;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(Espaco.m),
        child: Row(
          children: [
            IconeMaterial(nome: item.nomeMaterial, tamanho: 32),
            const SizedBox(width: Espaco.s),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(item.nomeMaterial, style: texto.titleSmall),
                  Text('Estimada: ${item.quantidadeEstimada.comUnidade(item.unidade)}', style: texto.bodyMedium),
                  Text(
                    efetiva == null
                        ? 'Recolhida: ainda não informada'
                        : 'Recolhida: ${efetiva.comUnidade(item.unidade)}',
                    style: texto.bodyMedium,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LinhaDoTempo extends StatelessWidget {
  const _LinhaDoTempo({required this.historico, required this.fuso, this.aoAbrirResponsavel});

  final List<Historico> historico;
  final FusoNegocio fuso;
  final void Function(TipoResponsavel tipo, int id)? aoAbrirResponsavel;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    if (historico.isEmpty) return const Text('Sem registros no histórico.');
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (var i = 0; i < historico.length; i++)
          IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                SizedBox(
                  width: 32,
                  child: Column(
                    children: [
                      Icon(
                        context.coresSituacao.solicitacao(historico[i].situacaoNova).icone,
                        size: 20,
                        color: context.coresSituacao.solicitacao(historico[i].situacaoNova).cor,
                      ),
                      if (i < historico.length - 1) const Expanded(child: VerticalDivider(width: 2, thickness: 2)),
                    ],
                  ),
                ),
                const SizedBox(width: Espaco.xs),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.only(bottom: Espaco.m),
                    child: MergeSemantics(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            historico[i].situacaoAnterior == null
                                ? 'Solicitação criada · ${historico[i].situacaoNova.rotulo}'
                                : '${historico[i].situacaoAnterior!.rotulo} → ${historico[i].situacaoNova.rotulo}',
                            style: texto.titleSmall,
                          ),
                          Text(
                            '${fuso.dataHora(historico[i].data)} · ${historico[i].responsavel}',
                            style: texto.bodySmall,
                          ),
                          if (historico[i].agendamento != null)
                            Text('Data marcada: ${fuso.dataHora(historico[i].agendamento!)}', style: texto.bodyMedium),
                          if (historico[i].motivo != null)
                            Text('Motivo: ${historico[i].motivo}', style: texto.bodyMedium),
                          if (historico[i].informacoesTentativa != null)
                            Text('Tentativa: ${historico[i].informacoesTentativa}', style: texto.bodyMedium),
                          if (aoAbrirResponsavel != null &&
                              historico[i].idResponsavel != null &&
                              historico[i].tipoResponsavel != TipoResponsavel.desconhecido)
                            TextButton(
                              onPressed: () =>
                                  aoAbrirResponsavel!(historico[i].tipoResponsavel, historico[i].idResponsavel!),
                              child: Text(
                                'Abrir cadastro',
                                semanticsLabel: 'Abrir cadastro de ${historico[i].responsavel}',
                              ),
                            ),
                        ],
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

/// Aviso após conflito ou falha de ação, com a mensagem do servidor.
Widget avisoConflito(String mensagem) => Aviso(tipo: TipoAviso.alerta, mensagem: mensagem);
