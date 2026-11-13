package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialDTO;
import br.edu.ifs.coletaseletiva.dto.Pagina;
import br.edu.ifs.coletaseletiva.entity.Material;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.ConflitoRegraException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.MaterialForm;
import br.edu.ifs.coletaseletiva.mapper.MaterialMapper;
import br.edu.ifs.coletaseletiva.repository.MaterialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);

    @Mock
    private MaterialRepository repository;

    private MaterialService service;

    @BeforeEach
    void preparar() {
        service = new MaterialService(repository, new MaterialMapper());
    }

    private static MaterialForm form(String nome, Unidade unidade, SituacaoCadastro situacao) {
        return new MaterialForm(nome, "Descrição", unidade, "Orientação", situacao);
    }

    private static Material vidro(SituacaoCadastro situacao) {
        return Material.builder().id(3).nome("Vidro").descricao("d").unidade(Unidade.KG).orientacao("o")
                .situacao(situacao).build();
    }

    // ---------- criar ----------

    @Test
    void rf03_criarSemSituacaoNasceAtivo() {
        when(repository.existsByNomeIgnoreCase("Vidro")).thenReturn(false);
        when(repository.save(any(Material.class))).thenAnswer(c -> {
            Material m = c.getArgument(0);
            m.setId(3);
            return m;
        });

        MaterialDTO dto = service.criar(ADMINISTRADOR, form("Vidro", Unidade.KG, null));

        assertEquals(3, dto.id());
        assertEquals(SituacaoCadastro.ATIVO, dto.situacao());
    }

    @Test
    void rf03_criarComSituacaoInativoRespeitaOCampo() {
        when(repository.existsByNomeIgnoreCase("Isopor")).thenReturn(false);
        when(repository.save(any(Material.class))).thenAnswer(c -> c.getArgument(0));

        MaterialDTO dto = service.criar(ADMINISTRADOR, form("Isopor", Unidade.KG, SituacaoCadastro.INATIVO));

        assertEquals(SituacaoCadastro.INATIVO, dto.situacao());
    }

    @Test
    void rf03_nomeRepetidoEhRejeitadoSemGravar() {
        when(repository.existsByNomeIgnoreCase("vidro")).thenReturn(true);

        ConflitoRegraException erro = assertThrows(ConflitoRegraException.class,
                () -> service.criar(ADMINISTRADOR, form("vidro", Unidade.KG, null)));

        assertEquals("erro.material.nome-repetido", erro.getChave());
        verify(repository, never()).save(any());
    }

    @Test
    void rf03_somenteAdministradorCriaEAtualiza() {
        Ator morador = new Ator(Perfil.MORADOR, 1);
        Ator operador = new Ator(Perfil.OPERADOR, 1);

        assertThrows(AcessoNegadoException.class, () -> service.criar(morador, form("Vidro", Unidade.KG, null)));
        assertThrows(AcessoNegadoException.class, () -> service.atualizar(operador, 3, form("Vidro", Unidade.KG, null)));

        verifyNoInteractions(repository);
    }

    // ---------- listar e buscar ----------

    @Test
    void rf03_consultaPublicaListaSomenteAtivosMesmoSePedirInativos() {
        when(repository.findBySituacao(any(SituacaoCadastro.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(vidro(SituacaoCadastro.ATIVO)), PageRequest.of(0, 20), 1));

        Pagina<MaterialDTO> pagina = service.listar(Optional.empty(), SituacaoCadastro.INATIVO, 0, 20);

        verify(repository).findBySituacao(org.mockito.ArgumentMatchers.eq(SituacaoCadastro.ATIVO), any(Pageable.class));
        assertEquals(1, pagina.totalElementos());
    }

    @Test
    void rf03_administradorSemFiltroListaTodosEComFiltroUsaASituacao() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(repository.findBySituacao(any(SituacaoCadastro.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listar(Optional.of(ADMINISTRADOR), null, 0, 20);
        service.listar(Optional.of(ADMINISTRADOR), SituacaoCadastro.INATIVO, 0, 20);

        verify(repository).findAll(any(Pageable.class));
        verify(repository).findBySituacao(org.mockito.ArgumentMatchers.eq(SituacaoCadastro.INATIVO), any(Pageable.class));
    }

    @Test
    void rf03_materialInativoEhNaoEncontradoParaQuemNaoEhAdministrador() {
        when(repository.findById(3)).thenReturn(Optional.of(vidro(SituacaoCadastro.INATIVO)));

        assertThrows(NaoEncontradoException.class, () -> service.buscar(Optional.empty(), 3));
        assertThrows(NaoEncontradoException.class,
                () -> service.buscar(Optional.of(new Ator(Perfil.MORADOR, 1)), 3));
        assertEquals(SituacaoCadastro.INATIVO, service.buscar(Optional.of(ADMINISTRADOR), 3).situacao());
    }

    // ---------- atualizar e ADR 0013 ----------

    @Test
    void rf03_atualizarSemMudarAUnidadeNaoConsultaDependenciasEPermiteInativar() {
        when(repository.findById(3)).thenReturn(Optional.of(vidro(SituacaoCadastro.ATIVO)));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Vidro", 3)).thenReturn(false);
        when(repository.save(any(Material.class))).thenAnswer(c -> c.getArgument(0));

        MaterialDTO dto = service.atualizar(ADMINISTRADOR, 3, form("Vidro", Unidade.KG, SituacaoCadastro.INATIVO));

        assertEquals(SituacaoCadastro.INATIVO, dto.situacao());
        verify(repository, never()).contarItensDeSolicitacao(any());
        verify(repository, never()).contarVinculosComPontos(any());
    }

    @Test
    void rf03_trocarAUnidadeSemDependenciasEPermitido() {
        when(repository.findById(3)).thenReturn(Optional.of(vidro(SituacaoCadastro.ATIVO)));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Vidro", 3)).thenReturn(false);
        when(repository.contarItensDeSolicitacao(3)).thenReturn(0L);
        when(repository.contarVinculosComPontos(3)).thenReturn(0L);
        when(repository.save(any(Material.class))).thenAnswer(c -> c.getArgument(0));

        MaterialDTO dto = service.atualizar(ADMINISTRADOR, 3, form("Vidro", Unidade.L, null));

        assertEquals(Unidade.L, dto.unidade());
    }

    @Test
    void rf03_trocarAUnidadeComItemOuComVinculoEBloqueado() {
        when(repository.findById(3)).thenReturn(Optional.of(vidro(SituacaoCadastro.ATIVO)));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Vidro", 3)).thenReturn(false);
        when(repository.contarItensDeSolicitacao(3)).thenReturn(1L);

        ConflitoRegraException porItem = assertThrows(ConflitoRegraException.class,
                () -> service.atualizar(ADMINISTRADOR, 3, form("Vidro", Unidade.L, null)));
        assertEquals("erro.material.unidade-bloqueada", porItem.getChave());

        when(repository.contarItensDeSolicitacao(3)).thenReturn(0L);
        when(repository.contarVinculosComPontos(3)).thenReturn(1L);
        assertThrows(ConflitoRegraException.class,
                () -> service.atualizar(ADMINISTRADOR, 3, form("Vidro", Unidade.UN, null)));

        verify(repository, never()).save(any());
    }

    @Test
    void rf03_nomeDeOutroMaterialEMaterialInexistenteSaoRejeitados() {
        when(repository.findById(3)).thenReturn(Optional.of(vidro(SituacaoCadastro.ATIVO)));
        when(repository.findById(99)).thenReturn(Optional.empty());
        when(repository.existsByNomeIgnoreCaseAndIdNot("Metal", 3)).thenReturn(true);

        assertThrows(ConflitoRegraException.class, () -> service.atualizar(ADMINISTRADOR, 3, form("Metal", Unidade.KG, null)));
        assertThrows(NaoEncontradoException.class, () -> service.atualizar(ADMINISTRADOR, 99, form("Metal", Unidade.KG, null)));
    }
}
