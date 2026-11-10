package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MoradorDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.entity.Morador;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.MoradorForm;
import br.edu.ifs.coletaseletiva.mapper.MoradorMapper;
import br.edu.ifs.coletaseletiva.repository.MoradorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoradorServiceTest {

    @Mock
    private MoradorRepository repository;

    private MoradorService service;

    @BeforeEach
    void preparar() {
        service = new MoradorService(repository, new MoradorMapper());
    }

    @Test
    void rf01_criarNormalizaOsDadosEGrava() {
        when(repository.existsByEmail("maria@exemplo.com")).thenReturn(false);
        when(repository.save(any(Morador.class))).thenAnswer(chamada -> {
            Morador m = chamada.getArgument(0);
            m.setId(1);
            return m;
        });

        MoradorDTO dto = service.criar(new MoradorForm("  Maria ", " Maria@Exemplo.COM ", "   ", null));

        assertEquals(1, dto.id());
        assertEquals("Maria", dto.nome());
        assertEquals("maria@exemplo.com", dto.email());
        assertNull(dto.fone());
        assertEquals(SituacaoCadastro.ATIVO, dto.situacao());
    }

    @Test
    void rf01_criarIgnoraASituacaoInformadaEOMoradorNasceAtivo() {
        when(repository.existsByEmail("maria@exemplo.com")).thenReturn(false);
        Morador[] gravado = new Morador[1];
        when(repository.save(any(Morador.class))).thenAnswer(chamada -> {
            gravado[0] = chamada.getArgument(0);
            gravado[0].setId(1);
            return gravado[0];
        });

        MoradorDTO dto = service.criar(new MoradorForm("Maria", "maria@exemplo.com", null, SituacaoCadastro.INATIVO));

        assertEquals(SituacaoCadastro.ATIVO, gravado[0].getSituacao());
        assertEquals(SituacaoCadastro.ATIVO, dto.situacao());
    }

    @Test
    void rf01_emailRepetidoEhRejeitadoSemGravar() {
        when(repository.existsByEmail("maria@exemplo.com")).thenReturn(true);

        assertThrows(ConflitoRegraException.class,
                () -> service.criar(new MoradorForm("Maria", "maria@exemplo.com", null, null)));

        verify(repository, never()).save(any());
    }

    @Test
    void rf01_moradorNaoPodeAlterarPropriaSituacao() {
        Morador existente = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));

        Ator ator = new Ator(Perfil.MORADOR, 1);
        MoradorForm form =
                new MoradorForm("Maria", "maria@exemplo.com", null, SituacaoCadastro.INATIVO);

        assertThrows(AcessoNegadoException.class, () -> service.atualizar(ator, 1, form));
        verify(repository, never()).save(any());
    }

    @Test
    void rf01_administradorInativaMorador() {
        Morador existente = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailAndIdNot("maria@exemplo.com", 1)).thenReturn(false);
        when(repository.save(any(Morador.class))).thenAnswer(chamada -> chamada.getArgument(0));

        Ator ator = new Ator(Perfil.ADMINISTRADOR, null);
        MoradorDTO dto = service.atualizar(ator, 1,
                new MoradorForm("Maria", "maria@exemplo.com", null, SituacaoCadastro.INATIVO));

        assertEquals(SituacaoCadastro.INATIVO, dto.situacao());
    }

    @Test
    void rf01_operadorNaoAcessaCadastroDeMorador() {
        Ator ator = new Ator(Perfil.OPERADOR, 7);

        assertThrows(AcessoNegadoException.class, () -> service.buscar(ator, 1));
    }

    // ---------- buscar ----------

    @Test
    void rf01_administradorBuscaQualquerMorador() {
        Morador existente = Morador.builder().id(5).nome("João").email("joao@exemplo.com").build();
        when(repository.findById(5)).thenReturn(Optional.of(existente));

        MoradorDTO dto = service.buscar(new Ator(Perfil.ADMINISTRADOR, null), 5);

        assertEquals(5, dto.id());
        assertEquals("joao@exemplo.com", dto.email());
    }

    @Test
    void rf01_buscarMoradorInexistenteLancaNaoEncontrado() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        Ator administrador = new Ator(Perfil.ADMINISTRADOR, null);

        assertThrows(NaoEncontradoException.class, () -> service.buscar(administrador, 99));
    }

    @Test
    void rf01_moradorBuscaOProprioCadastroAtivo() {
        Morador existente = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));

        MoradorDTO dto = service.buscar(new Ator(Perfil.MORADOR, 1), 1);

        assertEquals(1, dto.id());
    }

    @Test
    void rf01_moradorNaoAcessaCadastroDeOutroMorador() {
        Ator ator = new Ator(Perfil.MORADOR, 1);

        assertThrows(AcessoNegadoException.class, () -> service.buscar(ator, 2));
        verify(repository, never()).findById(any());
    }

    @Test
    void rf01_moradorInativoNaoAcessaOProprioCadastro() {
        Morador inativo = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com")
                .situacao(SituacaoCadastro.INATIVO).build();
        when(repository.findById(1)).thenReturn(Optional.of(inativo));
        Ator ator = new Ator(Perfil.MORADOR, 1);

        assertThrows(AcessoNegadoException.class, () -> service.buscar(ator, 1));
    }

    // ---------- listar ----------

    @Test
    void rf01_listarExigeAdministrador() {
        Ator ator = new Ator(Perfil.MORADOR, 1);

        assertThrows(AcessoNegadoException.class, () -> service.listar(ator, 0, 20));
        verify(repository, never()).findAll(any(Pageable.class));
    }

    @Test
    void rf01_listarPedePaginaOrdenadaPorIdEMontaOEnvelope() {
        Morador existente = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com").build();
        when(repository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(existente), PageRequest.of(2, 10), 21));

        Pagina<MoradorDTO> pagina = service.listar(new Ator(Perfil.ADMINISTRADOR, null), 2, 10);

        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(pedido.capture());
        assertEquals(2, pedido.getValue().getPageNumber());
        assertEquals(10, pedido.getValue().getPageSize());
        assertNotNull(pedido.getValue().getSort().getOrderFor("id"));

        assertEquals(1, pagina.conteudo().size());
        assertEquals(21L, pagina.totalElementos());
        assertEquals(3, pagina.totalPaginas());
    }

    // ---------- atualizar ----------

    @Test
    void rf01_atualizarSemSituacaoMantemASituacaoAtual() {
        Morador inativo = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com")
                .situacao(SituacaoCadastro.INATIVO).build();
        when(repository.findById(1)).thenReturn(Optional.of(inativo));
        when(repository.existsByEmailAndIdNot("maria@exemplo.com", 1)).thenReturn(false);
        when(repository.save(any(Morador.class))).thenAnswer(chamada -> chamada.getArgument(0));

        MoradorDTO dto = service.atualizar(new Ator(Perfil.ADMINISTRADOR, null), 1,
                new MoradorForm("Maria Silva", "maria@exemplo.com", null, null));

        assertEquals("Maria Silva", dto.nome());
        assertEquals(SituacaoCadastro.INATIVO, dto.situacao());
    }

    @Test
    void rf01_atualizarComEmailDeOutroMoradorLancaConflito() {
        Morador existente = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailAndIdNot("joao@exemplo.com", 1)).thenReturn(true);
        Ator administrador = new Ator(Perfil.ADMINISTRADOR, null);
        MoradorForm form = new MoradorForm("Maria", "joao@exemplo.com", null, null);

        assertThrows(ConflitoRegraException.class, () -> service.atualizar(administrador, 1, form));
        verify(repository, never()).save(any());
    }

    @Test
    void rf01_moradorAtualizaOProprioCadastroSemMudarASituacao() {
        Morador existente = Morador.builder().id(1).nome("Maria").email("maria@exemplo.com").build();
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByEmailAndIdNot("maria@exemplo.com", 1)).thenReturn(false);
        when(repository.save(any(Morador.class))).thenAnswer(chamada -> chamada.getArgument(0));

        MoradorDTO dto = service.atualizar(new Ator(Perfil.MORADOR, 1), 1,
                new MoradorForm("Maria Silva", "maria@exemplo.com", "79999990000",
                        SituacaoCadastro.ATIVO));

        assertEquals("Maria Silva", dto.nome());
        assertEquals("79999990000", dto.fone());
        assertEquals(SituacaoCadastro.ATIVO, dto.situacao());
    }

    @Test
    void rf01_atualizarMoradorInexistenteLancaNaoEncontradoSemGravar() {
        when(repository.findById(99)).thenReturn(Optional.empty());
        Ator administrador = new Ator(Perfil.ADMINISTRADOR, null);
        MoradorForm form = new MoradorForm("Maria", "maria@exemplo.com", null, null);

        assertThrows(NaoEncontradoException.class, () -> service.atualizar(administrador, 99, form));
        verify(repository, never()).save(any());
    }
}