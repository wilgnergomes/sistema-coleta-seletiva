package br.edu.ifs.coletaseletiva.entity;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoSolicitacao;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sol_solicitacao")
public class Solicitacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sol_nr_id")
    private Integer id;

    @Column(name = "sol_tx_protocolo", nullable = false, length = 30)
    private String protocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mor_nr_id", nullable = false)
    private Morador morador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poc_nr_id", nullable = false)
    private PontoColeta ponto;

    /** Texto copiado do endereço no momento do pedido (ADR 0008): mudanças no cadastro não o alteram. */
    @Column(name = "sol_tx_endereco_copia", nullable = false, length = 400)
    private String enderecoCopia;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "sol_tx_situacao", nullable = false, length = 20)
    private SituacaoSolicitacao situacao = SituacaoSolicitacao.SOLICITADA;

    @Column(name = "sol_dt_criacao", nullable = false, updatable = false)
    private OffsetDateTime criadaEm;

    @Column(name = "sol_dt_agendamento")
    private OffsetDateTime agendadaPara;

    @Column(name = "sol_dt_conclusao")
    private OffsetDateTime concluidaEm;

    @Column(name = "sol_tx_observacoes", length = 500)
    private String observacoes;

    /**
     * Controle de concorrência otimista (ADR 0009): o Hibernate soma 1 a cada alteração e recusa a gravação
     * se outra transação mudou a linha no meio (OptimisticLockException, que vira 409).
     */
    @Version
    @Column(name = "sol_nr_versao", nullable = false)
    private Integer versao;

    /**
     * Os itens nascem e morrem com a solicitação: cascade = ALL grava os itens junto, e orphanRemoval apaga
     * o item que for retirado da lista.
     */
    @Builder.Default
    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemSolicitacao> itens = new ArrayList<>();
}
