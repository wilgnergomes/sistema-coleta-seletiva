import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../ui/compartilhado/marca.dart';
import '../ui/compartilhado/shell.dart';
import 'sessao.dart';
import 'tema/tokens.dart';

const destinosPublicos = [
  DestinoNavegacao(rotulo: 'Pontos', icone: Icons.place_outlined, iconeSelecionado: Icons.place, rota: '/pontos'),
  DestinoNavegacao(
    rotulo: 'Materiais',
    icone: Icons.recycling_outlined,
    iconeSelecionado: Icons.recycling,
    rota: '/materiais',
  ),
];

const destinosMorador = [
  DestinoNavegacao(rotulo: 'Início', icone: Icons.home_outlined, iconeSelecionado: Icons.home, rota: '/morador/inicio'),
  DestinoNavegacao(rotulo: 'Pontos', icone: Icons.place_outlined, iconeSelecionado: Icons.place, rota: '/pontos'),
  DestinoNavegacao(
    rotulo: 'Solicitações',
    icone: Icons.receipt_long_outlined,
    iconeSelecionado: Icons.receipt_long,
    rota: '/morador/solicitacoes',
  ),
  DestinoNavegacao(
    rotulo: 'Conta',
    icone: Icons.person_outline,
    iconeSelecionado: Icons.person,
    rota: '/morador/conta',
  ),
];

const destinosOperador = [
  DestinoNavegacao(
    rotulo: 'Fila do ponto',
    icone: Icons.inbox_outlined,
    iconeSelecionado: Icons.inbox,
    rota: '/operador/fila',
  ),
];

const destinosAdministrador = [
  DestinoNavegacao(
    rotulo: 'Resumo',
    icone: Icons.insights_outlined,
    iconeSelecionado: Icons.insights,
    rota: '/administracao/resumo',
  ),
  DestinoNavegacao(
    rotulo: 'Solicitações',
    icone: Icons.receipt_long_outlined,
    iconeSelecionado: Icons.receipt_long,
    rota: '/administracao/solicitacoes',
  ),
  DestinoNavegacao(
    rotulo: 'Pontos',
    icone: Icons.place_outlined,
    iconeSelecionado: Icons.place,
    rota: '/administracao/pontos',
  ),
  DestinoNavegacao(
    rotulo: 'Materiais',
    icone: Icons.recycling_outlined,
    iconeSelecionado: Icons.recycling,
    rota: '/administracao/materiais',
  ),
  DestinoNavegacao(
    rotulo: 'Operadores',
    icone: Icons.badge_outlined,
    iconeSelecionado: Icons.badge,
    rota: '/administracao/operadores',
  ),
  DestinoNavegacao(
    rotulo: 'Moradores',
    icone: Icons.groups_outlined,
    iconeSelecionado: Icons.groups,
    rota: '/administracao/moradores',
  ),
];

int? indiceDestino(List<DestinoNavegacao> destinos, String caminho) {
  for (var i = 0; i < destinos.length; i++) {
    final rota = destinos[i].rota;
    if (caminho == rota || caminho.startsWith('$rota/')) return i;
  }
  // O morador vê o detalhe e os materiais dentro do destino Pontos.
  if (caminho.startsWith('/materiais')) {
    final pontos = destinos.indexWhere((d) => d.rota == '/pontos');
    return pontos < 0 ? null : pontos;
  }
  return null;
}

/// Estrutura da aplicação conforme o contexto: público, morador, operador
/// ou administrador. Mesma identidade visual e componentes, navegação própria.
class ShellApp extends StatelessWidget {
  const ShellApp({
    super.key,
    required this.sessao,
    required this.estado,
    required this.filho,
    required this.demonstracao,
  });

  final Sessao sessao;
  final GoRouterState estado;
  final Widget filho;
  final bool demonstracao;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: sessao,
      builder: (context, _) {
        final caminho = estado.uri.path;
        final area = Perfil.daRota(caminho) ?? (sessao.perfil == Perfil.morador ? Perfil.morador : null);
        final (destinos, compacta) = switch (area) {
          Perfil.morador => (destinosMorador, NavegacaoCompacta.barraInferior),
          Perfil.operador => (destinosOperador, NavegacaoCompacta.cabecalho),
          Perfil.administrador => (destinosAdministrador, NavegacaoCompacta.gaveta),
          null => (destinosPublicos, NavegacaoCompacta.cabecalho),
        };
        final compacto = MediaQuery.sizeOf(context).width < 600;
        return ShellAdaptativo(
          titulo: Marca(comNome: !(compacto && area == Perfil.administrador)),
          destinos: destinos,
          compacta: compacta,
          indiceSelecionado: indiceDestino(destinos, caminho),
          aoSelecionar: (i) => context.go(destinos[i].rota),
          faixa: sessao.ator == null || area == null ? null : _FaixaSimulacao(ator: sessao.ator!),
          acoes: area == null ? _acoesPublicas(context, compacto) : const [],
          corpo: filho,
        );
      },
    );
  }

  List<Widget> _acoesPublicas(BuildContext context, bool compacto) {
    final ator = sessao.ator;
    if (compacto) {
      return [
        PopupMenuButton<String>(
          tooltip: 'Menu',
          icon: const Icon(Icons.menu),
          onSelected: (rota) => context.go(rota),
          itemBuilder: (context) => [
            for (final d in destinosPublicos) PopupMenuItem(value: d.rota, child: Text(d.rotulo)),
            const PopupMenuItem(value: '/cadastro', child: Text('Cadastrar morador')),
            if (demonstracao)
              PopupMenuItem(
                value: ator == null ? '/demonstracao' : ator.perfil.rotaInicial,
                child: Text(ator == null ? 'Demonstração' : 'Minha área'),
              ),
          ],
        ),
      ];
    }
    return [
      TextButton(onPressed: () => context.go('/cadastro'), child: const Text('Cadastrar morador')),
      if (demonstracao)
        ator == null
            ? OutlinedButton.icon(
                onPressed: () => context.go('/demonstracao'),
                icon: const Icon(Icons.person_pin_outlined),
                label: const Text('Demonstração'),
              )
            : FilledButton.icon(
                onPressed: () => context.go(ator.perfil.rotaInicial),
                icon: const Icon(Icons.arrow_forward),
                label: const Text('Minha área'),
              ),
    ];
  }
}

/// Faixa visível nas áreas restritas: deixa claro que a identidade é
/// simulada e permite trocá-la.
class _FaixaSimulacao extends StatelessWidget {
  const _FaixaSimulacao({required this.ator});

  final Ator ator;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return Semantics(
      container: true,
      label: 'Simulação de identidade: ${ator.descricao}',
      child: Container(
        width: double.infinity,
        color: Cores.alertaFundo,
        padding: const EdgeInsets.symmetric(horizontal: Espaco.m, vertical: Espaco.xxs),
        child: Row(
          children: [
            const ExcludeSemantics(child: Icon(Icons.theater_comedy_outlined, size: 18, color: Cores.alerta)),
            const SizedBox(width: Espaco.xs),
            Expanded(
              child: ExcludeSemantics(
                child: Text(
                  'Simulação de identidade · ${ator.descricao}',
                  style: texto.labelMedium?.copyWith(color: Cores.alerta),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ),
            TextButton(
              style: TextButton.styleFrom(foregroundColor: Cores.alerta, minimumSize: const Size(48, 40)),
              onPressed: () => context.go('/demonstracao'),
              child: const Text('Trocar'),
            ),
          ],
        ),
      ),
    );
  }
}
