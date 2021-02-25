# librAIry API

&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
[![GitHub Issues](https://img.shields.io/github/issues/cbadenes/phd-thesis.svg)](https://github.com/cbadenes/phd-thesis/issues)
[![License](https://img.shields.io/badge/license-Apache2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![DOI](https://zenodo.org/badge/174326322.svg)](https://zenodo.org/badge/latestdoi/174326322)




***librAIry*** allows you to explore collections of documents on a large scale from a semantic point of view. It is based on the use of natural language processing (NLP) techniques combined with machine learning algorithms. 

Use of the service requires prior authentication. If you do not have an account, you must request it by email to 'cbadenes' at 'fi.upm.es' .

Multiple levels of depth are considered: (1) from the collection itself, discovering the topics that are mainly dealt with, (2) from a document, establishing content relations with other documents in the collection and (3) from a free text, identifying the most similar documents.


## Topic Model-as-a-Service

A topic model can easily be created from a `.csv` or `.jsonl` file (even in `.tar.gz` format), or from a SOLR collection. 

The model is distributed as a Docker container with a Rest API that allows to explore the topics and make inferences from the collection of documents (e.g a model created from the 20newsgroup dataset is available at [http://librairy.linkeddata.es/20news-model](http://librairy.linkeddata.es/20news-model)) 

Just make a HTTP_POST request to [http://librairy.linkeddata.es/api/topics](http://librairy.linkeddata.es/api) with a message like this: 

```json
{
  "name": "20newsgroup-model",
  "description": "topic model created from 20newsgroup dataset",
  "contactEmail": "personal@mail",
  "version": "1.0",
  "parameters": {
    "maxdocratio": "0.9",
    "minfreq": "5"
  },
  "docker": {
    "email": "docker-hub@mail",
    "password": "secret",
    "repository": "organization/name",
    "user": "dockerhub-user"
  },
  "dataSource": {
    "dataFields": {
      "id": "0",
      "labels": [
        "1"
      ],
      "text": [
        "2"
      ]
    },
    "filter":";;",
    "format": "CSV_TAR_GZ",
    "offset": 0,
    "size": 200,
    "url": "https://delicias.dia.fi.upm.es/nextcloud/index.php/s/mdeYNdiRs5obfMH/download"
  }
}

```

When a `dataSource` is CSV (i.e. `format=CSV` or `format=CSV_TAR_GZ`), the `filter` field is the separator used in the file (e.g. `;;`). In another case it is used as a filter on the query, for example when reading data from a collection in SOLR:

```json
{
  "name": "my-model",
  "description": "my first model using librAIry",
  "contactEmail": "sample@mail.com",
  "version": "1.3",
  "parameters": {
    "maxdocratio": "0.9",
    "minfreq": "5"    
  },
  "docker": {
    "email": "docker-hub@mail",
    "password": "secret",
    "repository": "organization/name",
    "user": "dockerhub-user"
  },
  "dataSource": {    
    "dataFields": {
      "id": "id",
      "labels": [
        "root-labels_t"
      ],
      "text": [
        "txt_t"
      ]
    },
    "filter":"size_i:[100 TO *] && source_s:public && lang_s:en && root-labels_t:[* TO *]",
    "format": "SOLR_CORE",
    "offset": 0,
    "size": 20000,
    "url": "http://localhost:8983/solr/documents"
  }
}

```

In any case, it is essential to correctly indicate the representative fields of the document (`dataFields`) by: 
- `id`: (***mandatory***) the field that identifies each document
- `text`: (***mandatory***) the field (or fields) that contain the text 
- `labels`: (***optional***) the field (or fields) that contain the labels that will define the topics themselves (supervised model).

An email account (`contactEmail`) and [dockerHub](https://hub.docker.com) credentials (`docker`) are required to notify and to publish the new model.

The following `parameters` can be set on request to adjust the model creation:

| Param    | Default  | Description |
| :------- |:--------:| :---------- |
| alpha    | 50/topics    | distributions of topics by document [0-100]|
| beta     | 0.1      | distributions of topics by word [0-100] |
| topics   | 10       | number of topics |
| iterations   | 1000       | number of iterations |
| maxdocratio   | 0.9       | maximum presence of a word in the collection  [0-1] |
| minfreq   | 5       | minimum presence of a word in the collection.  Number of documents |
| seed      | 1066       | oriented to the reproducibility of the model |
| language   | *from text*       | text language |
| lowercase   | true       | text previously normalized to lowercase |
| pos   | NOUN VERB ADJECTIVE       | valid part-of-speech tags|
| stopwords   | *empty*       | words not used in the model. List separated by whitespace |
| stoplabels   | *empty*       | labels not used in the model. List separated by whitespace |
| autowords   |  true      | automatic discovery of stopwords based on their presence in the topics |
| autolabels   |  true      | automatic discovery of stoplabels based on their presence in the documents |
| topwords   |  10      | number of words representing a topic |


More details about this message in the **TopicsRequest** section at [http://librairy.linkeddata.es/api](http://librairy.linkeddata.es/api)


## Documents Indexing

**librAIry** easily integrates with document storage systems such as [Solr](https://lucene.apache.org/solr/).

If you have a set of documents in CSV or JSONL (even in `.tar.gz`) format an easy way to index them in Solr is through the following HTTP_POST request to [http://librairy.linkeddata.es/api/documents](http://librairy.linkeddata.es/api):

```json 
{
  "contactEmail": "personal@mail",
  "dataSink": {
    "format": "SOLR_CORE",
    "url": "http://solr/documents"
  },
  "dataSource": {
    "dataFields": {
      "id": "1",
      "labels": [
        "0"
      ],
      "text": [
        "2"
      ]
    },
    "filter":",",
    "format": "CSV_TAR_GZ",
    "offset": 0,
    "size": -1,
    "url": "https://delicias.dia.fi.upm.es/nextcloud/index.php/s/mdeYNdiRs5obfMH/download"
  }
}
```

As before, you need to specify a `contactEmail` to let you know when the task has been executed.


## Semantic Annotation
 
 **librAIry** analyzes each document and annotates it based on its content automatically. This allows to establish semantic relationships between them (even in different languages) and explore corpus documents from their topics.  
 
 Given a collection of documents (`dataSource`) in cvs or jsonl format, or indexed in a solr server, along with a model (`modelEndpoint`), simply make the following HTTP_POST request to [http://librairy.linkeddata.es/api/annotations](http://librairy.linkeddata.es/api):
  
```json
{
  "contactEmail": "personal@mail",
  "dataSink": {
    "format": "SOLR_CORE",
    "url": "http://solr/documents"
  },
  "dataSource": {
    "dataFields": {
      "id": "id",
      "name": "name_s",
      "labels":[ "labels_t" ],
      "text": [
        "txt_t"
      ]
    },
    "filter": "source_s:ted && size_i:[10 TO *] && lang_s:en",
    "format": "SOLR_CORE",
    "offset": 0,
    "size": -1,
    "url": "http://solr/tbfy"
  },
  "modelEndpoint": "http://librairy.linkeddata.es/jrc-en-model"
}  
```

## Semantic Document Recommender

Given a document (`reference.document`), or free text (`reference.text`), **librAIry** identifies those documents most related based on their content.

If, for example, we want to obtain the document most related to the following text 'Hardware to support theatre IT project', it would be enough to make the following HTTP_POST request to [http://librairy.linkeddata.es/api/items](http://librairy.linkeddata.es/api):

```json
{
  "dataSource": {
    "dataFields": {
      "id": "id",
      "name": "name_s"
    },
    "filter": "source_s:oo-api AND lang_s:en",
    "format": "SOLR_CORE",
    "offset": 0,
    "size": -1,
    "url": "http://librairy.linkeddata.es/solr/tbfy"
  },
  "reference": {
    "text": {
      "model": "http://librairy.linkeddata.es/jrc-en-model",
      "content": "Hardware to support theatre IT project"
    }
  },
  "size": 10
}
```

If we want to get the list of documents from a given one, the query would be:

```json
{
  "dataSource": {
    "dataFields": {
      "id": "id",
      "name": "name_s"
    },
    "filter": "source_s:oo-api AND lang_s:en",
    "format": "SOLR_CORE",
    "offset": 0,
    "size": -1,
    "url": "http://librairy.linkeddata.es/solr/tbfy"
  },
  "reference": {
    "document": {
      "id": "ocds-b5fd17-e515b922-89fb-44f8-b0a0-1805a31726f2-270911-aw-dcs-hardware"
    }
  },
  "size": 10
}
```
