package es.upm.oeg.librairy.api.model;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class Document {

    private static final Logger LOG = LoggerFactory.getLogger(Document.class);

    private String id;

    private String name;

    private String text;

    private List<String> labels = new ArrayList<>();

    private Map<String,String> extraData = new HashMap<>();

    private String format;

    private String lang;

    private String source;

    private String date;

    private String file;

    public Document() {
    }

    public Document(String id, String text) {
        this.id = id;
        this.text = text;
    }

    public Document(String id, String text, List<String> labels) {
        this.id = id;
        this.text = text;
        this.labels = labels;
    }

    public boolean isValid(){
        return !Strings.isNullOrEmpty(id) && !Strings.isNullOrEmpty(text);
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels;
    }

    public Map<String, String> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, String> extraData) {
        this.extraData = extraData;
    }

    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", labels=" + labels +
                ", format='" + format + '\'' +
                ", lang='" + lang + '\'' +
                ", source='" + source + '\'' +
                ", date='" + date + '\'' +
                ", file='" + file + '\'' +
                '}';
    }
}
