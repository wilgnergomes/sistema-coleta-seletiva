package br.edu.ifs.coletaseletiva.service;

import br.edu.ifs.coletaseletiva.config.Ator;
import br.edu.ifs.coletaseletiva.dto.MaterialAceitoDTO;
import br.edu.ifs.coletaseletiva.entity.Material;
import br.edu.ifs.coletaseletiva.entity.MaterialAceito;
import br.edu.ifs.coletaseletiva.entity.MaterialAceitoId;
import br.edu.ifs.coletaseletiva.entity.PontoColeta;
import br.edu.ifs.coletaseletiva.enumeration.Perfil;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import br.edu.ifs.coletaseletiva.exception.AcessoNegadoException;
import br.edu.ifs.coletaseletiva.exception.NaoEncontradoException;
import br.edu.ifs.coletaseletiva.form.VinculoForm;
import br.edu.ifs.coletaseletiva.mapper.MaterialAceitoMapper;
import br.edu.ifs.coletaseletiva.repository.MaterialAceitoRepository;
import br.edu.ifs.coletaseletiva.repository.MaterialRepository;
import br.edu.ifs.coletaseletiva.repository.PontoColetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterialAceitoServiceTest {

    private static final Ator ADMINISTRADOR = new Ator(Perfil.ADMINISTRADOR, null);
    private static final MaterialAceitoId ID = new MaterialAceitoId(1, 2);

    @Mock
    private MaterialAceitoRepository repository;
    @Mock
    private PontoColetaRepository pontoRepository;
    @Mock
    private MaterialRepository materialRepository;

    private MaterialAceitoService service;
    private PontoColeta ponto;
    private Material vidro;

    @BeforeEach
    void preparar() {
        service = new MaterialAceitoService(repository, pontoRepository, materialRepository, new MaterialAceitoMapper());
        ponto = PontoColeta.builder().id(1).nome("Ponto").build();
        vidro = Material.builder().id(2).nome("Vidro").unidade(Unidade.KG).orientacao("Lavar").build();
    }

    @Test
    void rf05_vincularCriaOVinculoQuandoNaoExiste() {
        when(pontoRepository.findById(1)).thenReturn(Optional.of(ponto));
        when(materialRepository.findById(2)).thenReturn(Optional.of(vidro));
        when(repository.findById(ID)).thenReturn(Optional.empty());
        when(repository.save(any(MaterialAceito.class))).thenAnswer(c -> c.getArgument(0));

        MaterialAceitoDTO dto = service.vincular(ADMINISTRADOR, 1, 2,
                new VinculoForm("  Apenas limpas ", new BigDecimal("50.5")));

        assertEquals(2, dto.materialId());
        assertEquals("Vidro", dto.nomeMaterial());
        assertEquals(Unidade.KG, dto.unidade());
        assertEquals("Apenas limpas", dto.observacoes());
        assertEquals(new BigDecimal("50.5"), dto.limite());
    }

    @Test
    void rf05_vincularDeNovoAtualizaOVinculoExistenteSemCriarOutro() {
        MaterialAceito existente = MaterialAceito.builder().id(ID).ponto(ponto).material(vidro)
                .observacoes("antiga").limite(new BigDecimal("10")).build();
        when(pontoRepository.findById(1)).thenReturn(Optional.of(ponto));
        when(materialRepository.findById(2)).thenReturn(Optional.of(vidro));
        when(repository.findById(ID)).thenReturn(Optional.of(existente));
        when(repository.save(any(MaterialAceito.class))).thenAnswer(c -> c.getArgument(0));

        MaterialAceitoDTO dto = service.vincular(ADMINISTRADOR, 1, 2, new VinculoForm("   ", null));

        assertNull(dto.observacoes());
        assertNull(dto.limite());
        verify(repository).save(existente);
    }

    @Test
    void rf05_soAdministradorVinculaEDesvincula() {
        Ator morador = new Ator(Perfil.MORADOR, 1);
        Ator operador = new Ator(Perfil.OPERADOR, 1);

        assertThrows(AcessoNegadoException.class, () -> service.vincular(morador, 1, 2, new VinculoForm(null, null)));
        assertThrows(AcessoNegadoException.class, () -> service.remover(operador, 1, 2));

        verifyNoInteractions(repository, pontoRepository, materialRepository);
    }

    @Test
    void rf05_pontoOuMaterialInexistenteLancaNaoEncontradoSemGravar() {
        when(pontoRepository.findById(99)).thenReturn(Optional.empty());
        when(pontoRepository.findById(1)).thenReturn(Optional.of(ponto));
        when(materialRepository.findById(99)).thenReturn(Optional.empty());

        NaoEncontradoException semPonto = assertThrows(NaoEncontradoException.class,
                () -> service.vincular(ADMINISTRADOR, 99, 2, new VinculoForm(null, null)));
        NaoEncontradoException semMaterial = assertThrows(NaoEncontradoException.class,
                () -> service.vincular(ADMINISTRADOR, 1, 99, new VinculoForm(null, null)));

        assertEquals("erro.ponto.nao-encontrado", semPonto.getChave());
        assertEquals("erro.material.nao-encontrado", semMaterial.getChave());
        verify(repository, never()).save(any());
    }

    @Test
    void rf05_removerApagaOVinculoEVinculoInexistenteEhNaoEncontrado() {
        MaterialAceito existente = MaterialAceito.builder().id(ID).ponto(ponto).material(vidro).build();
        when(repository.findById(ID)).thenReturn(Optional.of(existente));
        when(repository.findById(new MaterialAceitoId(1, 3))).thenReturn(Optional.empty());

        service.remover(ADMINISTRADOR, 1, 2);

        verify(repository).delete(existente);
        assertThrows(NaoEncontradoException.class, () -> service.remover(ADMINISTRADOR, 1, 3));
    }
}
