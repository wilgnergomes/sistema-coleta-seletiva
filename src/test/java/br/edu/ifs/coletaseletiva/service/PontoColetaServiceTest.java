package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.PontoColetaDTO;
import br.edu.ifs.coletaseletiva.entity.EnderecoPonto;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.EnderecoForm;
import br.edu.ifs.coletaseletiva.form.PontoColetaForm;
import br.edu.ifs.coletaseletiva.mapper.PontoColetaMapper;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PontoColetaServiceTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);

    @Mock
    private PontoColetaRepository repository;

    private PontoColetaService service;

    @BeforeEach
    void preparar() {
        service = new PontoColetaService(repository, new PontoColetaMapper());
    }

    private static EnderecoForm endereco(String numero, String complemento) {
        return new EnderecoForm("Avenida Principal", numero, "Centro", "Lagarto", "SE", "49400000", complemento);
    }

    private static PontoColeta ponto(SituacaoCadastro situacao) {
        return PontoColeta.builder().id(1).nome("Ponto").situacao(situacao)
                .endereco(EnderecoPonto.builder().logradouro("R").numero("1").bairro("B").municipio("M")
                        .uf("SE").cep("49400000").build())
                .build();
    }

    @Test
    void rf04_criarNormalizaContatoHorariosEEnderecoEUsaPadroes() {
        when(repository.save(any(PontoColeta.class))).thenAnswer(c -> {
            PontoColeta p = c.getArgument(0);
            p.setId(1);
            return p;
        });

        PontoColetaDTO dto = service.criar(ADMINISTRADOR,
                new PontoColetaForm("  Ecoponto ", "   ", "", null, null, endereco("s/n", " ")));

        assertEquals("Ecoponto", dto.nome());
        assertNull(dto.contato());
        assertNull(dto.horarios());
        assertFalse(dto.recolhimentoDomiciliar());
        assertEquals(SituacaoCadastro.ATIVO, dto.situacao());
        assertEquals("S/N", dto.endereco().numero());
        assertNull(dto.endereco().complemento());
        assertTrue(dto.materiaisAceitos().isEmpty());
    }

    @Test
    void rf04_somenteAdministradorCriaEAtualiza() {
        PontoColetaForm form = new PontoColetaForm("P", null, null, true, null, endereco("1", null));

        assertThrows(AcessoNegadoException.class, () -> service.criar(new Ator(Perfil.MORADOR, 1), form));
        assertThrows(AcessoNegadoException.class, () -> service.atualizar(new Ator(Perfil.OPERADOR, 1), 1, form));

        verifyNoInteractions(repository);
    }

    @Test
    void rf04_consultaPublicaListaSomenteAtivosEAdministradorFiltra() {
        when(repository.findBySituacao(any(SituacaoCadastro.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ponto(SituacaoCadastro.ATIVO))));
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.listar(Optional.empty(), SituacaoCadastro.INATIVO, 0, 20);
        verify(repository).findBySituacao(eq(SituacaoCadastro.ATIVO), any(Pageable.class));

        service.listar(Optional.of(ADMINISTRADOR), null, 0, 20);
        verify(repository).findAll(any(Pageable.class));

        service.listar(Optional.of(ADMINISTRADOR), SituacaoCadastro.INATIVO, 0, 20);
        verify(repository).findBySituacao(eq(SituacaoCadastro.INATIVO), any(Pageable.class));
    }

    @Test
    void rf04_pontoInativoEhNaoEncontradoParaQuemNaoEhAdministrador() {
        when(repository.findById(1)).thenReturn(Optional.of(ponto(SituacaoCadastro.INATIVO)));

        assertThrows(NaoEncontradoException.class, () -> service.buscar(Optional.empty(), 1));
        assertEquals(SituacaoCadastro.INATIVO, service.buscar(Optional.of(ADMINISTRADOR), 1).situacao());
    }

    @Test
    void rf04_atualizarAlteraCamposEInativa() {
        when(repository.findById(1)).thenReturn(Optional.of(ponto(SituacaoCadastro.ATIVO)));
        when(repository.save(any(PontoColeta.class))).thenAnswer(c -> c.getArgument(0));

        PontoColetaDTO dto = service.atualizar(ADMINISTRADOR, 1,
                new PontoColetaForm("Novo nome", "(79) 3333-0000", null, true, SituacaoCadastro.INATIVO,
                        endereco("45", "Fundos")));

        assertEquals("Novo nome", dto.nome());
        assertTrue(dto.recolhimentoDomiciliar());
        assertEquals(SituacaoCadastro.INATIVO, dto.situacao());
        assertEquals("Fundos", dto.endereco().complemento());
    }

    @Test
    void rf04_atualizarPontoInexistenteLancaNaoEncontrado() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        PontoColetaForm form = new PontoColetaForm("P", null, null, null, null, endereco("1", null));

        assertThrows(NaoEncontradoException.class, () -> service.atualizar(ADMINISTRADOR, 99, form));
    }
}
