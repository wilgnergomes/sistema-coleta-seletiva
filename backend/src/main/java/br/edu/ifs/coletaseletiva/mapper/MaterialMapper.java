package br.edu.ifs.coletaseletiva.mapper;

import br.edu.ifs.coletaseletiva.dto.MaterialDTO;
import br.edu.ifs.coletaseletiva.entity.Material;
import br.edu.ifs.coletaseletiva.form.MaterialForm;
import org.springframework.stereotype.Component;

@Component
public class MaterialMapper {

    public Material paraEntidade(MaterialForm form) {
        Material material = new Material();
        atualizar(material, form);
        return material;
    }

    /** Copia os campos do form; a situação só muda se vier informada. */
    public void atualizar(Material material, MaterialForm form) {
        material.setNome(form.nome());
        material.setDescricao(form.descricao());
        material.setUnidade(form.unidade());
        material.setOrientacao(form.orientacao());
        if (form.situacao() != null) {
            material.setSituacao(form.situacao());
        }
    }

    public MaterialDTO paraDTO(Material material) {
        return new MaterialDTO(
                material.getId(),
                material.getNome(),
                material.getDescricao(),
                material.getUnidade(),
                material.getOrientacao(),
                material.getSituacao()
        );
    }
}
