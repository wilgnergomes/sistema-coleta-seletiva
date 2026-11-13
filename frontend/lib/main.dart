import 'package:material_ui/material_ui.dart';

import 'app/app.dart';
import 'app/dependencias.dart';
import 'app/rotas.dart';

void main() {
  final dependencias = Dependencias.doAmbiente();
  runApp(AppColetaSeletiva(rotas: criarRotas(dependencias)));
}
