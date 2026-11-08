package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.OperadorDTO;
import br.edu.ifs.coletaseletiva.entity.Operador;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.OperadorForm;
import br.edu.ifs.coletaseletiva.mapper.OperadorMapper;
import br.edu.ifs.coletaseletiva.repository.OperadorRepository;
import br.edu.ifs.coletaseletiva.repository.PontoColetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperadorServiceTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);

    @Mock
    private OperadorRepository repository;
    @Mock
    private PontoColetaRepository pontoRepository;

    private OperadorService service;
    private PontoColeta ponto;

    @BeforeEach
    void preparar() {
        service = new OperadorService(repository, pontoRepository, new OperadorMapper());
        ponto = PontoColeta.builder().id(1).nome("Ponto").build();
    }

    private static OperadorForm form(String email, int pontoId, SituacaoCadastro situacao) {
        return new OperadorForm("Carla", email, pontoId, situacao);
    }

    @Test
    void rf07_criarNormalizaEmailVinculaAoPontoENasceAtivo() {
        when(pontoRepository.findById(1)).thenReturn(Optional.of(ponto));
        when(repository.existsByEmail("carla@exemplo.com")).thenReturn(false);
        when(repository.save(any(Operador.class))).thenAnswer(c -> {
            Operador o = c.getArgument(0);
            o.setId(7);
            return o;
        });

        OperadorDTO dto = service.criar(ADMINISTRADOR, form("  Carla@Exemplo.COM ", 1, null));

        assertEquals(7, dto.id());
        assertEquals("carla@exemplo.com", dto.email());
        assertEquals(1, dto.pontoId());
        assertEquals(SituacaoCadastro.ATIVO, dto.situacao());
    }

    @Test
    void rf07_emailRepetidoEPontoInexistenteSaoRejeitadosSemGravar() {
        when(pontoRepository.findById(1)).thenReturn(Optional.of(ponto));
        when(pontoRepository.findById(99)).thenReturn(Optional.empty());
        when(repository.existsByEmail("carla@exemplo.com")).thenReturn(true);

        ConflitoRegraException conflito = assertThrows(ConflitoRegraException.class,
                () -> service.criar(ADMINISTRADOR, form("carla@exemplo.com", 1, null)));
        NaoEncontradoException semPonto = assertThrows(NaoEncontradoException.class,
                () -> service.criar(ADMINISTRADOR, form("outra@exemplo.com", 99, null)));

        assertEquals("erro.operador.email-repetido", conflito.getChave());
        assertEquals("erro.ponto.nao-encontrado", semPonto.getChave());
        verify(repository, never()).save(any());
    }

    @Test
    void rf07_somenteAdministradorAcessaTodasAsOperacoes() {
        Ator operador = new Ator(Perfil.OPERADOR, 1);
        Ator morador = new Ator(Perfil.MORADOR, 1);

        assertThrows(AcessoNegadoException.class, () -> service.criar(operador, form("a@exemplo.com", 1, null)));
        assertThrows(AcessoNegadoException.class, () -> service.listar(morador, null, 0, 20));
        assertThrows(AcessoNegadoException.class, () -> service.buscar(operador, 1));
        assertThrows(AcessoNegadoException.class, () -> service.atualizar(morador, 1, form("a@exemplo.com", 1, null)));

        verifyNoInteractions(repository, pontoRepository);
    }

    @Test
    void rf07_listarUsaOFiltroDePontoQuandoInformado() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(repository.findByPontoId(eq(1), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.listar(ADMINISTRADOR, null, 0, 20);
        service.listar(ADMINISTRADOR, 1, 0, 20);

        verify(repository).findAll(any(Pageable.class));
        verify(repository).findByPontoId(eq(1), any(Pageable.class));
    }

    @Test
    void rf07_buscarOperadorInexistenteLancaNaoEncontrado() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NaoEncontradoException.class, () -> service.buscar(ADMINISTRADOR, 99));
    }

    @Test
    void rf07_atualizarMudaDePontoEInativa() {
        PontoColeta outro = PontoColeta.builder().id(2).nome("Outro").build();
        Operador existente = Operador.builder().id(7).nome("Carla").email("carla@exemplo.com").ponto(ponto).build();
        when(repository.findById(7)).thenReturn(Optional.of(existente));
        when(pontoRepository.findById(2)).thenReturn(Optional.of(outro));
        when(repository.existsByEmailAndIdNot("carla@exemplo.com", 7)).thenReturn(false);
        when(repository.save(any(Operador.class))).thenAnswer(c -> c.getArgument(0));

        OperadorDTO dto = service.atualizar(ADMINISTRADOR, 7, form("carla@exemplo.com", 2, SituacaoCadastro.INATIVO));

        assertEquals(2, dto.pontoId());
        assertEquals(SituacaoCadastro.INATIVO, dto.situacao());
    }

    @Test
    void rf07_atualizarComEmailDeOutroOperadorLancaConflito() {
        Operador existente = Operador.builder().id(7).nome("Carla").email("carla@exemplo.com").ponto(ponto).build();
        when(repository.findById(7)).thenReturn(Optional.of(existente));
        when(pontoRepository.findById(1)).thenReturn(Optional.of(ponto));
        when(repository.existsByEmailAndIdNot("paulo@exemplo.com", 7)).thenReturn(true);

        assertThrows(ConflitoRegraException.class,
                () -> service.atualizar(ADMINISTRADOR, 7, form("Paulo@Exemplo.com", 1, null)));
        verify(repository, never()).save(any());
    }
}
