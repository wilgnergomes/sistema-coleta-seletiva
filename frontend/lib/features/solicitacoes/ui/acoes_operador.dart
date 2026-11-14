import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/dominio/enums.dart';
import '../../../ui/compartilhado/estados.dart';
import '../data/solicitacoes.dart';
import 'componentes_solicitacao.dart';
import 'formulario_conclusao.dart';
import '../../../ui/compartilhado/dialogos.dart';
import 'formularios_acao.dart';
import 'view_models.dart';
import '../../../ui/compartilhado/superficie.dart';

/// Ação disponível para o operador na situação atual.
class AcaoOperador {
  const AcaoOperador(this.rotulo, this.icone, this.executar, {this.principal = false});

  final String rotulo;
  final IconData icone;
  final Future<void> Function(BuildContext context) executar;
  final bool principal;
}

/// Ações do operador do ponto conforme a situação (tabela de transições).
/// A principal fica visível perto da situação; no compacto as demais vão
/// para `Mais ações`.
class AcoesOperador extends StatelessWidget {
  const AcoesOperador({super.key, required this.viewModel, required this.aoAlterar, this.agora});

  final DetalheSolicitacaoViewModel viewModel;
  final VoidCallback aoAlterar;

  /// Relógio injetável para testes.
  final DateTime Function()? agora;

  Future<void> _executar(Future<SolicitacaoDetalhe> Function(SolicitacaoDetalhe atual) acao, String sucesso) async {
    final ok = await viewModel.executar(acao, sucesso: sucesso);
    // Sucesso ou conflito podem mudar a fila filtrada.
    if (ok || viewModel.tipoAviso == TipoAviso.alerta) aoAlterar();
  }

  List<AcaoOperador> acoesPara(SolicitacaoDetalhe d) {
    final repo = viewModel.repositorio;
    final fuso = viewModel.fuso;
    final agendar = AcaoOperador('Agendar', Icons.event_available_outlined, principal: true, (context) async {
      final dados = await pedirDataHora(
        context,
        fuso: fuso,
        titulo: 'Agendar recolhimento',
        descricao: 'Escolha quando o recolhimento de ${d.protocolo} será feito.',
        rotuloAcao: 'Agendar',
        agora: agora,
      );
      if (dados == null) return;
      await _executar((a) => repo.agendar(a.id, dados.instante, a.versao), 'Recolhimento agendado.');
    });
    final recusar = AcaoOperador('Recusar', Icons.block, (context) async {
      final motivo = await pedirMotivo(
        context,
        titulo: 'Recusar solicitação',
        descricao: 'A solicitação ${d.protocolo} será recusada. O morador verá o motivo no histórico.',
        rotuloAcao: 'Recusar solicitação',
      );
      if (motivo == null) return;
      await _executar((a) => repo.recusar(a.id, motivo, a.versao), 'Solicitação recusada.');
    });
    final cancelar = AcaoOperador('Cancelar', Icons.cancel_outlined, (context) async {
      final motivo = await pedirMotivo(
        context,
        titulo: 'Cancelar solicitação',
        descricao:
            'A solicitação ${d.protocolo} será cancelada.'
            '${d.agendadaPara == null ? '' : ' A data agendada continua registrada no histórico.'}',
        rotuloAcao: 'Cancelar solicitação',
      );
      if (motivo == null) return;
      await _executar((a) => repo.cancelar(a.id, motivo, a.versao), 'Solicitação cancelada.');
    });
    final iniciar = AcaoOperador('Iniciar recolhimento', Icons.play_arrow_outlined, principal: true, (context) async {
      final ok = await confirmar(
        context,
        titulo: 'Iniciar recolhimento',
        mensagem:
            'Confirme o início do recolhimento de ${d.protocolo}.'
            '${d.agendadaPara == null ? '' : ' Agendado para ${fuso.dataHora(d.agendadaPara!)}.'}',
        rotuloAcao: 'Iniciar recolhimento',
      );
      if (!ok) return;
      await _executar((a) => repo.iniciar(a.id, a.versao), 'Recolhimento iniciado.');
    });
    final reagendar = AcaoOperador('Reagendar', Icons.edit_calendar_outlined, (context) async {
      final dados = await pedirDataHora(
        context,
        fuso: fuso,
        titulo: 'Reagendar recolhimento',
        descricao: 'Escolha a nova data de ${d.protocolo} e informe o motivo. A data anterior fica no histórico.',
        rotuloAcao: 'Reagendar',
        pedirMotivo: true,
        atual: d.agendadaPara,
        diferenteDe: d.agendadaPara,
        agora: agora,
      );
      if (dados == null) return;
      await _executar((a) => repo.reagendar(a.id, dados.instante, dados.motivo!, a.versao), 'Recolhimento reagendado.');
    });
    final concluir = AcaoOperador('Concluir recolhimento', Icons.task_alt, principal: true, (context) async {
      final efetivas = await pedirQuantidadesEfetivas(context, d);
      if (efetivas == null) return;
      await _executar((a) => repo.concluir(a.id, efetivas, a.versao), 'Recolhimento concluído.');
    });
    final falha = AcaoOperador('Registrar falha', Icons.report_problem_outlined, (context) async {
      final dados = await pedirDataHora(
        context,
        fuso: fuso,
        titulo: 'Registrar tentativa sem sucesso',
        descricao:
            'A solicitação ${d.protocolo} volta para Agendada na nova data. '
            'O motivo e as informações ficam no histórico.',
        rotuloAcao: 'Registrar falha',
        pedirMotivo: true,
        pedirInformacoes: true,
        atual: d.agendadaPara,
        agora: agora,
      );
      if (dados == null) return;
      await _executar(
        (a) => repo.registrarFalha(
          a.id,
          motivo: dados.motivo!,
          informacoesTentativa: dados.informacoes!,
          novaDataHora: dados.instante,
          versao: a.versao,
        ),
        'Tentativa sem sucesso registrada. Novo agendamento salvo.',
      );
    });
    return switch (d.situacao) {
      SituacaoSolicitacao.solicitada => [agendar, recusar, cancelar],
      SituacaoSolicitacao.agendada => [iniciar, reagendar, cancelar],
      SituacaoSolicitacao.emRecolhimento => [concluir, falha],
      _ => const [],
    };
  }

  @override
  Widget build(BuildContext context) {
    final d = viewModel.carga.dados;
    if (d == null) return const SizedBox.shrink();
    final acoes = acoesPara(d);
    if (acoes.isEmpty) {
      return Text(
        d.situacao.finalizada ? 'Situação final: não há ações disponíveis.' : 'Nenhuma ação disponível nesta situação.',
        style: Theme.of(context).textTheme.bodyMedium,
      );
    }
    final principal = acoes.where((a) => a.principal).firstOrNull;
    final secundarias = acoes.where((a) => !a.principal).toList();
    final desabilitado = viewModel.enviando;
    return LayoutBuilder(
      builder: (context, restricoes) {
        final compacto = restricoes.maxWidth < 480;
        Widget botao(AcaoOperador a) => a.principal
            ? Elevavel(
                child: FilledButton.icon(
                  onPressed: desabilitado ? null : () => a.executar(context),
                  icon: Icon(a.icone),
                  label: Text(a.rotulo),
                ),
              )
            : OutlinedButton.icon(
                onPressed: desabilitado ? null : () => a.executar(context),
                icon: Icon(a.icone),
                label: Text(a.rotulo),
              );
        if (!compacto || secundarias.length < 2) {
          return Wrap(spacing: Espaco.s, runSpacing: Espaco.s, children: [for (final a in acoes) botao(a)]);
        }
        return Wrap(
          spacing: Espaco.s,
          runSpacing: Espaco.s,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            if (principal != null) botao(principal),
            MenuAnchor(
              builder: (context, controle, _) => OutlinedButton.icon(
                onPressed: desabilitado ? null : () => controle.isOpen ? controle.close() : controle.open(),
                icon: const Icon(Icons.more_horiz),
                label: const Text('Mais ações'),
              ),
              menuChildren: [
                for (final a in secundarias)
                  MenuItemButton(
                    leadingIcon: Icon(a.icone),
                    onPressed: () => a.executar(context),
                    child: Text(a.rotulo),
                  ),
              ],
            ),
          ],
        );
      },
    );
  }
}
