import '../../../core/api/json.dart';
import '../../../core/dominio/enums.dart';
import '../../../core/dominio/quantidade.dart';

class MaterialColeta {
  const MaterialColeta({
    required this.id,
    required this.nome,
    required this.descricao,
    required this.unidade,
    required this.orientacao,
    required this.situacao,
  });

  factory MaterialColeta.deJson(Json json) => MaterialColeta(
    id: json.inteiro('id'),
    nome: json.texto('nome'),
    descricao: json.textoOpcional('descricao') ?? '',
    unidade: Unidade.deApi(json.textoOpcional('unidade')),
    orientacao: json.textoOpcional('orientacao') ?? '',
    situacao: SituacaoCadastro.deApi(json.textoOpcional('situacao')),
  );

  final int id;
  final String nome;
  final String descricao;
  final Unidade unidade;
  final String orientacao;
  final SituacaoCadastro situacao;

  bool get ativo => situacao == SituacaoCadastro.ativo;

  Json paraJson() => {
    'nome': nome,
    'descricao': descricao,
    'unidade': unidade.api,
    'orientacao': orientacao,
    'situacao': situacao.api,
  };
}

/// Endereço embutido do ponto e campos do `EnderecoForm`.
class Endereco {
  const Endereco({
    required this.logradouro,
    required this.numero,
    required this.bairro,
    required this.municipio,
    required this.uf,
    required this.cep,
    this.complemento,
  });

  factory Endereco.deJson(Json json) => Endereco(
    logradouro: json.texto('logradouro'),
    numero: json.texto('numero'),
    bairro: json.texto('bairro'),
    municipio: json.texto('municipio'),
    uf: json.texto('uf'),
    cep: json.texto('cep'),
    complemento: json.textoOpcional('complemento'),
  );

  final String logradouro;
  final String numero;
  final String bairro;
  final String municipio;
  final String uf;
  final String cep;
  final String? complemento;

  String get linha1 =>
      ['$logradouro, $numero', if (complemento != null && complemento!.trim().isNotEmpty) complemento!].join(' · ');

  String get linha2 => '$bairro · $municipio/$uf · CEP ${cepFormatado(cep)}';

  String get completo => '$linha1 · $linha2';

  Json paraJson() => {
    'logradouro': logradouro,
    'numero': numero,
    'bairro': bairro,
    'municipio': municipio,
    'uf': uf,
    'cep': cep,
    'complemento': (complemento == null || complemento!.trim().isEmpty) ? null : complemento,
  };
}

String cepFormatado(String cep) => cep.length == 8 ? '${cep.substring(0, 5)}-${cep.substring(5)}' : cep;

class MaterialAceito {
  const MaterialAceito({
    required this.materialId,
    required this.nomeMaterial,
    required this.unidade,
    required this.orientacao,
    this.observacoes,
    this.limite,
  });

  factory MaterialAceito.deJson(Json json) => MaterialAceito(
    materialId: json.inteiro('materialId'),
    nomeMaterial: json.texto('nomeMaterial'),
    unidade: Unidade.deApi(json.textoOpcional('unidade')),
    orientacao: json.textoOpcional('orientacao') ?? '',
    observacoes: json.textoOpcional('observacoes'),
    limite: json.numeroOpcional('limite') == null ? null : Quantidade.deNumero(json.numero('limite')),
  );

  final int materialId;
  final String nomeMaterial;
  final Unidade unidade;
  final String orientacao;
  final String? observacoes;

  /// Quantidade máxima por solicitação. Nulo quando não há limite.
  final Quantidade? limite;
}

class PontoColeta {
  const PontoColeta({
    required this.id,
    required this.nome,
    required this.recolhimentoDomiciliar,
    required this.situacao,
    required this.endereco,
    required this.materiaisAceitos,
    this.contato,
    this.horarios,
  });

  factory PontoColeta.deJson(Json json) => PontoColeta(
    id: json.inteiro('id'),
    nome: json.texto('nome'),
    contato: json.textoOpcional('contato'),
    horarios: json.textoOpcional('horarios'),
    recolhimentoDomiciliar: json['recolhimentoDomiciliar'] == true,
    situacao: SituacaoCadastro.deApi(json.textoOpcional('situacao')),
    endereco: Endereco.deJson(json.mapa('endereco')),
    materiaisAceitos: json.listaOuVazia('materiaisAceitos', MaterialAceito.deJson),
  );

  final int id;
  final String nome;
  final String? contato;
  final String? horarios;
  final bool recolhimentoDomiciliar;
  final SituacaoCadastro situacao;
  final Endereco endereco;
  final List<MaterialAceito> materiaisAceitos;

  bool get ativo => situacao == SituacaoCadastro.ativo;

  /// O ponto aceita novas solicitações de recolhimento domiciliar.
  bool get aceitaSolicitacao => ativo && recolhimentoDomiciliar;

  String get modalidade => recolhimentoDomiciliar ? 'Recolhimento domiciliar' : 'Entrega no local';
}

/// Filtros do RF06, refletidos na URL.
class FiltroPontos {
  const FiltroPontos({this.nome, this.municipio, this.bairro, this.materialId, this.situacao, this.pagina = 0});

  /// Lê os parâmetros da URL, ignorando valores inválidos.
  factory FiltroPontos.deQuery(Map<String, String> query) {
    String? texto(String chave) {
      final valor = query[chave]?.trim();
      return valor == null || valor.isEmpty ? null : valor;
    }

    final pagina = int.tryParse(query['pagina'] ?? '') ?? 1;
    final materialId = int.tryParse(query['materialId'] ?? '');
    final situacao = SituacaoCadastro.deApi(query['situacao']);
    return FiltroPontos(
      nome: texto('nome'),
      municipio: texto('municipio'),
      bairro: texto('bairro'),
      materialId: materialId != null && materialId > 0 ? materialId : null,
      situacao: situacao == SituacaoCadastro.desconhecida ? null : situacao,
      pagina: pagina < 1 ? 0 : pagina - 1,
    );
  }

  final String? nome;
  final String? municipio;
  final String? bairro;
  final int? materialId;

  /// Só o administrador filtra por situação.
  final SituacaoCadastro? situacao;

  /// Página a partir de 0.
  final int pagina;

  bool get temFiltros => nome != null || municipio != null || bairro != null || materialId != null || situacao != null;

  /// Parâmetros da URL da interface, com página a partir de 1.
  Map<String, String> paraUrl() => {
    'nome': ?nome,
    'municipio': ?municipio,
    'bairro': ?bairro,
    if (materialId != null) 'materialId': '$materialId',
    if (situacao != null) 'situacao': situacao!.api,
    if (pagina > 0) 'pagina': '${pagina + 1}',
  };

  /// Parâmetros da API.
  Map<String, Object?> paraApi({int tamanho = 20}) => {
    'nome': nome,
    'municipio': municipio,
    'bairro': bairro,
    'materialId': materialId,
    'situacao': situacao?.api,
    'page': pagina,
    'size': tamanho,
  };

  FiltroPontos naPagina(int pagina) => FiltroPontos(
    nome: nome,
    municipio: municipio,
    bairro: bairro,
    materialId: materialId,
    situacao: situacao,
    pagina: pagina,
  );

  FiltroPontos semMaterial() => FiltroPontos(nome: nome, municipio: municipio, bairro: bairro, situacao: situacao);

  @override
  bool operator ==(Object other) =>
      other is FiltroPontos &&
      other.nome == nome &&
      other.municipio == municipio &&
      other.bairro == bairro &&
      other.materialId == materialId &&
      other.situacao == situacao &&
      other.pagina == pagina;

  @override
  int get hashCode => Object.hash(nome, municipio, bairro, materialId, situacao, pagina);
}
