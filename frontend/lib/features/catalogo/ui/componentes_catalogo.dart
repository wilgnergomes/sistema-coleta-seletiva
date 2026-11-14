import 'package:material_ui/material_ui.dart';

import '../../../app/tema/tokens.dart';
import '../../../core/dominio/enums.dart';
import '../../../ui/compartilhado/dialogos.dart';
import '../../../ui/compartilhado/estados.dart';
import '../../../ui/compartilhado/material_visual.dart';
import '../../../ui/compartilhado/superficie.dart';
import '../data/modelos.dart';
import '../data/repositorio_catalogo.dart';

/// Barra de pesquisa larga: busca à esquerda, nome do ponto no centro e
/// filtros à direita, dentro da própria barra.
class BarraPesquisa extends StatelessWidget {
  const BarraPesquisa({
    super.key,
    required this.controlador,
    required this.aoPesquisar,
    required this.aoAbrirFiltros,
    this.quantidadeFiltros = 0,
  });

  final TextEditingController controlador;
  final ValueChanged<String> aoPesquisar;
  final VoidCallback aoAbrirFiltros;

  /// Filtros secundários aplicados (município, bairro, material).
  final int quantidadeFiltros;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controlador,
      textInputAction: TextInputAction.search,
      onSubmitted: aoPesquisar,
      maxLength: 120,
      decoration: InputDecoration(
        hintText: 'Pesquisar pelo nome do ponto',
        labelText: 'Nome do ponto',
        floatingLabelBehavior: FloatingLabelBehavior.never,
        counterText: '',
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(Raio.pilula)),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(Raio.pilula),
          borderSide: const BorderSide(color: Cores.contorno),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(Raio.pilula),
          borderSide: const BorderSide(color: Cores.foco, width: 2),
        ),
        contentPadding: const EdgeInsets.symmetric(vertical: 14),
        prefixIcon: IconButton(
          tooltip: 'Buscar',
          icon: const Icon(Icons.search),
          onPressed: () => aoPesquisar(controlador.text),
        ),
        suffixIcon: Padding(
          padding: const EdgeInsets.only(right: Espaco.xxs),
          child: IconButton(
            tooltip: quantidadeFiltros == 0 ? 'Filtros' : 'Filtros ($quantidadeFiltros aplicados)',
            onPressed: aoAbrirFiltros,
            icon: Badge(
              isLabelVisible: quantidadeFiltros > 0,
              label: Text('$quantidadeFiltros'),
              backgroundColor: Cores.primario,
              child: const Icon(Icons.tune),
            ),
          ),
        ),
      ),
    );
  }
}

/// Atalhos circulares para os quatro materiais tradicionais e acesso aos
/// demais. A API aceita um material por consulta, então a seleção é única.
class AtalhosMateriais extends StatelessWidget {
  const AtalhosMateriais({super.key, required this.catalogo, required this.selecionado, required this.aoSelecionar});

  final CatalogoMateriais? catalogo;
  final int? selecionado;

  /// Recebe o material escolhido, ou nulo para limpar.
  final ValueChanged<MaterialColeta?> aoSelecionar;

  @override
  Widget build(BuildContext context) {
    final itens = catalogo?.itens ?? const <MaterialColeta>[];
    final tradicionais = [
      for (final nome in materiaisTradicionais)
        ?itens.where((m) => normalizarNome(m.nome) == normalizarNome(nome)).firstOrNull,
    ];
    final outros = itens.where((m) => !tradicionais.contains(m)).toList();
    final outroSelecionado = outros.where((m) => m.id == selecionado).firstOrNull;
    return Semantics(
      container: true,
      label: 'Pesquisar por material',
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final m in tradicionais)
              _Atalho(
                rotulo: m.nome,
                selecionado: m.id == selecionado,
                icone: IconeMaterial(nome: m.nome, tamanho: 52),
                aoTocar: () => aoSelecionar(m.id == selecionado ? null : m),
              ),
            if (outros.isNotEmpty)
              _Atalho(
                rotulo: outroSelecionado?.nome ?? 'Outros',
                selecionado: outroSelecionado != null,
                icone: Container(
                  width: 52,
                  height: 52,
                  decoration: const BoxDecoration(color: Cores.sucessoFundo, shape: BoxShape.circle),
                  child: const Icon(Icons.more_horiz, color: Cores.primario),
                ),
                aoTocar: () async {
                  final escolhido = await escolherMaterial(context, outros, selecionado: selecionado);
                  if (escolhido != null) aoSelecionar(escolhido.id == selecionado ? null : escolhido);
                },
              ),
          ],
        ),
      ),
    );
  }
}

class _Atalho extends StatelessWidget {
  const _Atalho({required this.rotulo, required this.selecionado, required this.icone, required this.aoTocar});

  final String rotulo;
  final bool selecionado;
  final Widget icone;
  final VoidCallback aoTocar;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      selected: selecionado,
      label: selecionado ? '$rotulo, selecionado' : rotulo,
      excludeSemantics: true,
      child: InkWell(
        onTap: aoTocar,
        borderRadius: BorderRadius.circular(Raio.cartao),
        child: SizedBox(
          width: 84,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: Espaco.xs),
            child: Column(
              children: [
                AnimatedContainer(
                  duration: const Duration(milliseconds: 120),
                  padding: const EdgeInsets.all(3),
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(color: selecionado ? Cores.texto : Colors.transparent, width: 2),
                  ),
                  child: icone,
                ),
                const SizedBox(height: Espaco.xxs),
                Text(
                  rotulo,
                  maxLines: 2,
                  textAlign: TextAlign.center,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelMedium
                      ?.copyWith(fontWeight: selecionado ? FontWeight.w600 : FontWeight.w400),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Lista de materiais em diálogo centralizado para escolha única.
Future<MaterialColeta?> escolherMaterial(BuildContext context, List<MaterialColeta> materiais, {int? selecionado}) {
  return abrirFormulario<MaterialColeta>(
    context,
    titulo: 'Escolha um material',
    longo: false,
    construir: (context) => ListView(
      shrinkWrap: true,
      padding: const EdgeInsets.only(bottom: Espaco.m),
      children: [
        for (final m in materiais)
          ListTile(
            contentPadding: const EdgeInsets.symmetric(horizontal: Espaco.l),
            leading: IconeMaterial(nome: m.nome, tamanho: 36),
            title: Text(m.nome),
            subtitle: Text('Medido em ${m.unidade.extenso}'),
            selected: m.id == selecionado,
            trailing: m.id == selecionado ? const Icon(Icons.check) : null,
            onTap: () => Navigator.of(context).pop(m),
          ),
      ],
    ),
  );
}

/// Filtros secundários do RF06: diálogo centralizado no amplo e tela
/// própria no celular.
Future<FiltroPontos?> abrirFiltrosPontos(
  BuildContext context, {
  required FiltroPontos atual,
  required CatalogoMateriais? catalogo,
  bool permitirSituacao = false,
}) {
  return abrirFormulario<FiltroPontos>(
    context,
    titulo: 'Filtros',
    construir: (_) => _FolhaFiltros(atual: atual, catalogo: catalogo, permitirSituacao: permitirSituacao),
  );
}

class _FolhaFiltros extends StatefulWidget {
  const _FolhaFiltros({required this.atual, required this.catalogo, required this.permitirSituacao});

  final FiltroPontos atual;
  final CatalogoMateriais? catalogo;
  final bool permitirSituacao;

  @override
  State<_FolhaFiltros> createState() => _FolhaFiltrosState();
}

class _FolhaFiltrosState extends State<_FolhaFiltros> {
  late final _nome = TextEditingController(text: widget.atual.nome);
  late final _municipio = TextEditingController(text: widget.atual.municipio);
  late final _bairro = TextEditingController(text: widget.atual.bairro);
  late int? _material = widget.atual.materialId;
  late String? _situacao = widget.atual.situacao?.api;

  @override
  void dispose() {
    _nome.dispose();
    _municipio.dispose();
    _bairro.dispose();
    super.dispose();
  }

  String? _texto(TextEditingController c) => c.text.trim().isEmpty ? null : c.text.trim();

  @override
  Widget build(BuildContext context) {
    final materiais = widget.catalogo?.itens ?? const <MaterialColeta>[];
    final materialConhecido = _material == null || materiais.any((m) => m.id == _material);
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(Espaco.l, Espaco.xs, Espaco.l, Espaco.l),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: Largura.formulario),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Município e bairro precisam do nome completo. Acentos contam.',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
                const SizedBox(height: Espaco.m),
                TextField(
                  controller: _nome,
                  maxLength: 120,
                  decoration: const InputDecoration(labelText: 'Nome do ponto', counterText: ''),
                ),
                const SizedBox(height: Espaco.m),
                TextField(
                  controller: _municipio,
                  maxLength: 100,
                  decoration: const InputDecoration(labelText: 'Município', counterText: ''),
                ),
                const SizedBox(height: Espaco.m),
                TextField(
                  controller: _bairro,
                  maxLength: 100,
                  decoration: const InputDecoration(labelText: 'Bairro', counterText: ''),
                ),
                const SizedBox(height: Espaco.m),
                DropdownButtonFormField<int?>(
                  initialValue: materialConhecido ? _material : null,
                  isExpanded: true,
                  decoration: const InputDecoration(labelText: 'Material aceito'),
                  items: [
                    const DropdownMenuItem(value: null, child: Text('Qualquer material')),
                    for (final m in materiais) DropdownMenuItem(value: m.id, child: Text(m.nome)),
                  ],
                  onChanged: (v) => setState(() => _material = v),
                ),
                if (widget.permitirSituacao) ...[
                  const SizedBox(height: Espaco.m),
                  DropdownButtonFormField<String?>(
                    initialValue: _situacao,
                    decoration: const InputDecoration(labelText: 'Situação'),
                    items: const [
                      DropdownMenuItem(value: null, child: Text('Todas')),
                      DropdownMenuItem(value: 'ATIVO', child: Text('Ativo')),
                      DropdownMenuItem(value: 'INATIVO', child: Text('Inativo')),
                    ],
                    onChanged: (v) => setState(() => _situacao = v),
                  ),
                ],
                const SizedBox(height: Espaco.l),
                Wrap(
                  alignment: WrapAlignment.end,
                  spacing: Espaco.s,
                  runSpacing: Espaco.s,
                  children: [
                    TextButton(
                      onPressed: () => Navigator.of(context).pop(const FiltroPontos()),
                      child: const Text('Limpar filtros'),
                    ),
                    FilledButton(
                      onPressed: () => Navigator.of(context).pop(
                        FiltroPontos(
                          nome: _texto(_nome),
                          municipio: _texto(_municipio),
                          bairro: _texto(_bairro),
                          materialId: _material,
                          situacao: _situacao == null ? null : FiltroPontos.deQuery({'situacao': _situacao!}).situacao,
                        ),
                      ),
                      child: const Text('Aplicar filtros'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Altura base dos cartões de ponto na mesma grade (multiplicada pela escala
/// de texto pelo [CartaoInterativo]).
const alturaCartaoPonto = 184.0;

/// Cartão de resultado: nome, localização, modalidade e acesso ao detalhe.
/// Altura fixa por grade; nomes e endereços longos são abreviados aqui e
/// aparecem completos no detalhe.
class CartaoPonto extends StatelessWidget {
  const CartaoPonto({super.key, required this.ponto, required this.aoAbrir, this.mostrarSituacao = false});

  final PontoColeta ponto;
  final VoidCallback aoAbrir;
  final bool mostrarSituacao;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    return CartaoInterativo(
      altura: alturaCartaoPonto,
      aoTocar: aoAbrir,
      rotuloSemantico: 'Ver detalhes de ${ponto.nome}',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(ponto.nome, style: texto.titleMedium, maxLines: 2, overflow: TextOverflow.ellipsis),
          const SizedBox(height: Espaco.xxs),
          Text(
            '${ponto.endereco.municipio} · ${ponto.endereco.bairro}',
            style: texto.bodyMedium?.copyWith(color: Cores.textoSecundario),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
          Text(ponto.endereco.linha1, style: texto.bodySmall, maxLines: 1, overflow: TextOverflow.ellipsis),
          const Spacer(),
          Row(
            children: [
              Flexible(child: SeloModalidade(domiciliar: ponto.recolhimentoDomiciliar)),
              if (mostrarSituacao && !ponto.ativo) ...[
                const SizedBox(width: Espaco.xs),
                const SeloCadastro(situacao: SituacaoCadastro.inativo),
              ],
              const SizedBox(width: Espaco.xs),
              const Spacer(),
              Flexible(
                child: ExcludeSemantics(
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Flexible(
                        child: Text(
                          'Ver detalhes',
                          style: texto.labelLarge?.copyWith(color: Cores.primario),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const Icon(Icons.chevron_right, color: Cores.primario, size: 20),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

/// Altura base dos cartões de material na mesma grade.
const alturaCartaoMaterial = 156.0;

/// Cartão de material com área de ícone, nome, unidade, descrição limitada a
/// duas linhas e acesso à orientação completa.
class CartaoMaterial extends StatelessWidget {
  const CartaoMaterial({super.key, required this.material, required this.aoAbrir, this.mostrarSituacao = false});

  final MaterialColeta material;
  final VoidCallback aoAbrir;
  final bool mostrarSituacao;

  @override
  Widget build(BuildContext context) {
    final texto = Theme.of(context).textTheme;
    final m = material;
    return CartaoInterativo(
      altura: alturaCartaoMaterial,
      aoTocar: aoAbrir,
      rotuloSemantico: mostrarSituacao ? 'Editar ${m.nome}' : 'Ver orientações de ${m.nome}',
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 56,
            child: Align(
              alignment: Alignment.topLeft,
              child: IconeMaterial(nome: m.nome, tamanho: 48),
            ),
          ),
          const SizedBox(width: Espaco.s),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(m.nome, style: texto.titleMedium, maxLines: 1, overflow: TextOverflow.ellipsis),
                    ),
                    if (mostrarSituacao) SeloCadastro(situacao: m.situacao),
                  ],
                ),
                Text(
                  'Medido em ${m.unidade.extenso} (${m.unidade.simbolo})',
                  style: texto.bodySmall,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: Espaco.xxs),
                Text(m.descricao, style: texto.bodyMedium, maxLines: 2, overflow: TextOverflow.ellipsis),
                const Spacer(),
                Align(
                  alignment: Alignment.centerRight,
                  child: ExcludeSemantics(
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Flexible(
                          child: Text(
                            mostrarSituacao ? 'Editar' : 'Ver orientações',
                            style: texto.labelLarge?.copyWith(color: Cores.primario),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        const Icon(Icons.chevron_right, color: Cores.primario, size: 20),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class SeloModalidade extends StatelessWidget {
  const SeloModalidade({super.key, required this.domiciliar});

  final bool domiciliar;

  @override
  Widget build(BuildContext context) {
    final cor = domiciliar ? Cores.sucesso : const Color(0xFF304E67);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: Espaco.xs, vertical: Espaco.xxs),
      decoration: BoxDecoration(
        color: domiciliar ? Cores.sucessoFundo : const Color(0xFFE8F0F8),
        borderRadius: BorderRadius.circular(Raio.campo),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          ExcludeSemantics(
            child: Icon(domiciliar ? Icons.local_shipping_outlined : Icons.storefront_outlined, size: 16, color: cor),
          ),
          const SizedBox(width: Espaco.xxs),
          Flexible(
            child: Text(
              domiciliar ? 'Recolhimento domiciliar' : 'Entrega no local',
              style: Theme.of(context).textTheme.labelMedium?.copyWith(color: cor),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}
