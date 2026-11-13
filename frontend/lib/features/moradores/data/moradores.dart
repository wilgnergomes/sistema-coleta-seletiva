import '../../../core/api/cliente_api.dart';
import '../../../core/api/json.dart';
import '../../../core/api/pagina.dart';
import '../../../core/dominio/enums.dart';
import '../../catalogo/data/modelos.dart';

class Morador {
  const Morador({required this.id, required this.nome, required this.email, required this.situacao, this.fone});

  factory Morador.deJson(Json json) => Morador(
    id: json.inteiro('id'),
    nome: json.texto('nome'),
    email: json.texto('email'),
    fone: json.textoOpcional('fone'),
    situacao: SituacaoCadastro.deApi(json.textoOpcional('situacao')),
  );

  final int id;
  final String nome;
  final String email;
  final String? fone;
  final SituacaoCadastro situacao;

  bool get ativo => situacao == SituacaoCadastro.ativo;
}

/// Corpo do `MoradorForm`. Sem [situacao], o servidor preserva a atual.
class DadosMorador {
  const DadosMorador({required this.nome, required this.email, this.fone, this.situacao});

  final String nome;
  final String email;
  final String? fone;
  final SituacaoCadastro? situacao;

  Json paraJson() => {
    'nome': nome.trim(),
    'email': email.trim(),
    'fone': fone == null || fone!.trim().isEmpty ? null : fone!.trim(),
    if (situacao != null) 'situacao': situacao!.api,
  };
}

class EnderecoMorador {
  const EnderecoMorador({required this.id, required this.moradorId, required this.endereco});

  factory EnderecoMorador.deJson(Json json) =>
      EnderecoMorador(id: json.inteiro('id'), moradorId: json.inteiro('moradorId'), endereco: Endereco.deJson(json));

  final int id;
  final int moradorId;
  final Endereco endereco;
}

/// Moradores e endereços (RF01, RF02).
class RepositorioMoradores {
  RepositorioMoradores(this._api);

  final ClienteApi _api;

  /// Cadastro público: sem cabeçalhos de identidade.
  Future<Morador> cadastrar(DadosMorador dados) async =>
      Morador.deJson(comoMapa(await _api.post('/moradores', corpo: dados.paraJson(), acesso: Acesso.publico)));

  Future<Morador> obter(int id, {Map<String, String>? identidade}) async =>
      Morador.deJson(comoMapa(await _api.get('/moradores/$id', acesso: Acesso.ator, identidade: identidade)));

  Future<Morador> atualizar(int id, DadosMorador dados) async =>
      Morador.deJson(comoMapa(await _api.put('/moradores/$id', corpo: dados.paraJson())));

  Future<Pagina<Morador>> listar({int pagina = 0, int tamanho = 20}) async => Pagina.deJson(
    await _api.get('/moradores', query: {'page': pagina, 'size': tamanho}, acesso: Acesso.ator),
    Morador.deJson,
  );

  Future<List<EnderecoMorador>> enderecos(int moradorId) async =>
      comoLista(await _api.get('/moradores/$moradorId/enderecos', acesso: Acesso.ator), EnderecoMorador.deJson);

  Future<EnderecoMorador> criarEndereco(int moradorId, Endereco endereco) async =>
      EnderecoMorador.deJson(comoMapa(await _api.post('/moradores/$moradorId/enderecos', corpo: endereco.paraJson())));

  Future<EnderecoMorador> atualizarEndereco(int moradorId, int id, Endereco endereco) async => EnderecoMorador.deJson(
    comoMapa(await _api.put('/moradores/$moradorId/enderecos/$id', corpo: endereco.paraJson())),
  );
}
