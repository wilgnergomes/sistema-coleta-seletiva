package br.edu.ifs.coletaseletiva.entity;

import br.edu.ifs.coletaseletiva.enumeration.SituacaoCadastro;
import br.edu.ifs.coletaseletiva.enumeration.Unidade;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mat_material")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mat_nr_id")
    private Integer id;

    @Column(name = "mat_tx_nome", nullable = false, length = 80)
    private String nome;

    @Column(name = "mat_tx_descricao", nullable = false, length = 500)
    private String descricao;

    /** EnumType.STRING grava o nome ("KG"), e não a posição (0, 1, 2): o texto não quebra se a ordem do enum mudar. */
    @Enumerated(EnumType.STRING)
    @Column(name = "mat_tx_unidade", nullable = false, length = 5)
    private Unidade unidade;

    @Column(name = "mat_tx_orientacao", nullable = false, length = 1000)
    private String orientacao;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "mat_tx_situacao", nullable = false, length = 10)
    private SituacaoCadastro situacao = SituacaoCadastro.ATIVO;
}
