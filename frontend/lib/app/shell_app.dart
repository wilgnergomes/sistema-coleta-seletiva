import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../ui/compartilhado/marca.dart';
import '../ui/compartilhado/shell.dart';
import 'sessao.dart';
import 'tema/tokens.dart';
import '../ui/compartilhado/superficie.dart';

const destinosPublicos = [
  DestinoNavegacao(rotulo: 'Início', icone: Icons.home_outlined, iconeSelecionado: Icons.home, rota: '/'),
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
    if (caminho == rota || (rota != '/' && caminho.startsWith('$rota/'))) return i;
  }
  // O morador vê o detalhe e os materiais dentro do destino Pontos.
  if (caminho.startsWith('/materiais')) {
    final pontos = destinos.indexWhere((d) => d.rota == '/pontos');
    return pontos < 0 ? null : pontos;
  }
  return null;
}

/// Área da navegação: a do prefixo da rota ou, para o morador, também as
/// páginas públicas que ele consulta (pontos e materiais).
Perfil? areaDaRota(String caminho, Perfil? perfil) =>
    Perfil.daRota(caminho) ?? (perfil == Perfil.morador ? Perfil.morador : null);

/// Destinos exibidos na navegação de cada área (nulo: área pública sem menu lateral).
List<DestinoNavegacao> destinosDaArea(Perfil? area) => switch (area) {
  Perfil.morador => destinosMorador,
  Perfil.operador => destinosOperador,
  Perfil.administrador => destinosAdministrador,
  null => const <DestinoNavegacao>[],
};

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
        final area = areaDaRota(caminho, sessao.perfil);
        final destinos = destinosDaArea(area);
        final compacta = switch (area) {
          Perfil.morador => NavegacaoCompacta.barraInferior,
          Perfil.administrador => NavegacaoCompacta.gaveta,
          Perfil.operador || null => NavegacaoCompacta.cabecalho,
        };
        final compacto = MediaQuery.sizeOf(context).width < 600;
        final cabecalhoCompacto = MediaQuery.sizeOf(context).width < 900;
        return ShellAdaptativo(
          titulo: Semantics(
            link: true,
            label: 'Coleta Seletiva, página inicial',
            child: InkWell(
              borderRadius: BorderRadius.circular(Raio.campo),
              onTap: () => context.go('/'),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: Espaco.xxs, vertical: Espaco.xs),
                child: Marca(comNome: !(compacto && area == Perfil.administrador)),
              ),
            ),
          ),
          destinos: destinos,
          compacta: compacta,
          indiceSelecionado: indiceDestino(destinos, caminho),
          aoSelecionar: (i) => context.go(destinos[i].rota),
          faixa: sessao.ator == null || area == null ? null : _FaixaSimulacao(sessao: sessao),
          acoes: area == null ? _acoesPublicas(context, cabecalhoCompacto, caminho) : const [],
          corpo: filho,
        );
      },
    );
  }

  List<Widget> _acoesPublicas(BuildContext context, bool compacto, String caminho) {
    final ator = sessao.ator;
    if (compacto) {
      return [
        PopupMenuButton<String>(
          tooltip: 'Menu',
          icon: const Icon(Icons.menu),
          onSelected: (rota) => context.go(rota),
          itemBuilder: (context) => [
            for (final d in destinosPublicos)
              PopupMenuItem(
                value: d.rota,
                child: ListTile(leading: Icon(d.icone), title: Text(d.rotulo), contentPadding: EdgeInsets.zero),
              ),
            const PopupMenuItem(
              value: '/cadastro',
              child: ListTile(
                leading: Icon(Icons.person_add_alt),
                title: Text('Cadastrar-se'),
                contentPadding: EdgeInsets.zero,
              ),
            ),
            if (demonstracao)
              PopupMenuItem(
                value: ator == null ? '/demonstracao' : ator.perfil.rotaInicial,
                child: ListTile(
                  leading: const Icon(Icons.play_circle_outline),
                  title: Text(ator == null ? 'Demonstração' : 'Minha área'),
                  contentPadding: EdgeInsets.zero,
                ),
              ),
          ],
        ),
      ];
    }
    final indice = indiceDestino(destinosPublicos, caminho);
    return [
      for (var i = 0; i < destinosPublicos.length; i++)
        _LinkCabecalho(
          rotulo: destinosPublicos[i].rotulo,
          selecionado: i == indice,
          aoTocar: () => context.go(destinosPublicos[i].rota),
        ),
      const SizedBox(width: Espaco.s),
      TextButton(onPressed: () => context.go('/cadastro'), child: const Text('Cadastrar-se')),
      const SizedBox(width: Espaco.xs),
      if (demonstracao)
        ator == null
            ? Elevavel(
                child: FilledButton.icon(
                  onPressed: () => context.go('/demonstracao'),
                  icon: const Icon(Icons.play_circle_outline),
                  label: const Text('Demonstração'),
                ),
              )
            : Elevavel(
                child: FilledButton.icon(
                  onPressed: () => context.go(ator.perfil.rotaInicial),
                  icon: const IconeDirecional(Icons.arrow_forward),
                  label: const Text('Minha área'),
                ),
              ),
    ];
  }
}

/// Link de navegação do cabeçalho público, com sublinhado no destino atual.
class _LinkCabecalho extends StatelessWidget {
  const _LinkCabecalho({required this.rotulo, required this.selecionado, required this.aoTocar});

  final String rotulo;
  final bool selecionado;
  final VoidCallback aoTocar;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      selected: selecionado,
      child: TextButton(
        style: TextButton.styleFrom(foregroundColor: selecionado ? Cores.primario : Cores.texto),
        onPressed: aoTocar,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(rotulo),
            AnimatedContainer(
              duration: const Duration(milliseconds: 220),
              margin: const EdgeInsets.only(top: 2),
              height: 2,
              width: selecionado ? 24 : 0,
              color: Cores.primario,
            ),
          ],
        ),
      ),
    );
  }
}

/// Faixa visível nas áreas restritas: deixa claro que a identidade é
/// simulada e permite trocá-la.
class _FaixaSimulacao extends StatelessWidget {
  const _FaixaSimulacao({required this.sessao});

  final Sessao sessao;

  /// Encerra a identidade simulada: a sessão limpa caches e respostas
  /// restritas (`aoTrocarAtor`), e a navegação volta à página inicial.
  void _sair(BuildContext context) {
    sessao.sair();
    context.go('/');
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(const SnackBar(content: Text('Você saiu da demonstração.')));
  }

  /// Troca de perfil: limpa o contexto atual antes da nova seleção.
  void _trocar(BuildContext context) {
    sessao.sair();
    context.go('/demonstracao');
  }

  @override
  Widget build(BuildContext context) {
    final ator = sessao.ator!;
    final texto = Theme.of(context).textTheme;
    final compacto = MediaQuery.sizeOf(context).width < 600;
    final estilo = TextButton.styleFrom(foregroundColor: Cores.alerta, minimumSize: const Size(48, 40));
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
            if (compacto)
              PopupMenuButton<String>(
                tooltip: 'Opções da demonstração',
                icon: const Icon(Icons.more_vert, color: Cores.alerta),
                onSelected: (opcao) => opcao == 'sair' ? _sair(context) : _trocar(context),
                itemBuilder: (context) => const [
                  PopupMenuItem(value: 'trocar', child: Text('Trocar perfil')),
                  PopupMenuItem(value: 'sair', child: Text('Sair da demonstração')),
                ],
              )
            else ...[
              TextButton.icon(
                style: estilo,
                onPressed: () => _trocar(context),
                icon: const Icon(Icons.switch_account_outlined, size: 18),
                label: const Text('Trocar perfil'),
              ),
              TextButton.icon(
                style: estilo,
                onPressed: () => _sair(context),
                icon: const Icon(Icons.logout, size: 18),
                label: const Text('Sair da demonstração'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
