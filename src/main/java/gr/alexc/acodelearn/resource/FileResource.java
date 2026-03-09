package gr.alexc.acodelearn.resource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_resource")
@Getter
@Setter
@NoArgsConstructor
public class FileResource extends Resource {

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "description")
    private String summary;

    @Column(name = "file_data")
    private byte[] fileData;
}
