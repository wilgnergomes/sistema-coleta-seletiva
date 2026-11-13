/// Leitura defensiva de JSON.
///
/// Os leitores toleram chaves adicionais e nulos em campos opcionais e
/// sinalizam violação de formato em campo obrigatório.
library;

import 'falhas.dart';

typedef Json = Map<String, Object?>;

Json comoMapa(Object? valor, [String contexto = 'resposta']) {
  if (valor is Map<String, Object?>) return valor;
  if (valor is Map) return valor.cast<String, Object?>();
  throw RespostaInvalida('$contexto não é um objeto');
}

List<T> comoLista<T>(Object? valor, T Function(Json) item, [String contexto = 'lista']) {
  if (valor is! List) throw RespostaInvalida('$contexto não é uma lista');
  return [for (final elemento in valor) item(comoMapa(elemento, contexto))];
}

extension LeituraJson on Json {
  Object? _exigir(String chave) {
    final valor = this[chave];
    if (valor == null) throw RespostaInvalida('campo obrigatório ausente: $chave');
    return valor;
  }

  String texto(String chave) {
    final valor = _exigir(chave);
    if (valor is String) return valor;
    throw RespostaInvalida('campo $chave não é texto');
  }

  String? textoOpcional(String chave) {
    final valor = this[chave];
    if (valor == null) return null;
    if (valor is String) return valor;
    throw RespostaInvalida('campo $chave não é texto');
  }

  int inteiro(String chave) {
    final valor = _exigir(chave);
    if (valor is int) return valor;
    if (valor is num && valor == valor.truncate()) return valor.toInt();
    throw RespostaInvalida('campo $chave não é inteiro');
  }

  int? inteiroOpcional(String chave) => this[chave] == null ? null : inteiro(chave);

  num numero(String chave) {
    final valor = _exigir(chave);
    if (valor is num) return valor;
    throw RespostaInvalida('campo $chave não é número');
  }

  num? numeroOpcional(String chave) => this[chave] == null ? null : numero(chave);

  bool booleano(String chave) {
    final valor = _exigir(chave);
    if (valor is bool) return valor;
    throw RespostaInvalida('campo $chave não é booleano');
  }

  /// Instante ISO-8601 com offset, convertido para UTC.
  DateTime instante(String chave) {
    final valor = texto(chave);
    final data = DateTime.tryParse(valor);
    if (data == null) throw RespostaInvalida('campo $chave não é data');
    return data.toUtc();
  }

  DateTime? instanteOpcional(String chave) => this[chave] == null ? null : instante(chave);

  Json mapa(String chave) => comoMapa(_exigir(chave), chave);

  Json? mapaOpcional(String chave) => this[chave] == null ? null : mapa(chave);

  List<T> lista<T>(String chave, T Function(Json) item) => comoLista(_exigir(chave), item, chave);

  /// Lista que pode vir ausente, tratada como vazia.
  List<T> listaOuVazia<T>(String chave, T Function(Json) item) => this[chave] == null ? <T>[] : lista(chave, item);
}
