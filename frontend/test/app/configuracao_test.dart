import 'package:coleta_seletiva_web/app/configuracao.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  ConfiguracaoApp config({String modo = 'demo', String base = '/api'}) =>
      ConfiguracaoApp.deTextos(modo: modo, apiBaseUrl: base, fusoNegocio: 'America/Sao_Paulo');

  test('interpreta os modos aceitos', () {
    expect(config(modo: 'demo').demonstracaoHabilitada, isTrue);
    expect(config(modo: 'publico').demonstracaoHabilitada, isFalse);
    expect(config(modo: '').modo, ModoApp.publico);
  });

  test('rejeita modo desconhecido e base vazia', () {
    expect(() => config(modo: 'producao'), throwsArgumentError);
    expect(() => config(base: ' '), throwsArgumentError);
  });

  test('base relativa usa a mesma origem da página, no PC e na rede local', () {
    expect(
      config().resolverBase(Uri.parse('http://127.0.0.1:8000/#/pontos?materialId=1')).toString(),
      'http://127.0.0.1:8000/api',
    );
    expect(config().resolverBase(Uri.parse('http://192.168.1.50:8000/')).toString(), 'http://192.168.1.50:8000/api');
  });

  test('base absoluta é preservada e perde a barra final', () {
    expect(
      config(base: 'http://10.0.2.2:8080/').resolverBase(Uri.parse('http://localhost/')).toString(),
      'http://10.0.2.2:8080',
    );
  });
}
