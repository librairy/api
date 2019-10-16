Steps to create a new 'core':  
1.- Create a directory with the name of the core 
    
    $ mkdir my-core  
2.- Grant all privileges to this directory 

    $ chmod -R +777 my-core  
3.- Copy the 'conf' folder into that directory 

    $ cp -r conf my-core/  
4.- Select `Core Admin/Add core` in the web Solr admin with the following parameters:  
    
    name: the name of the core, it should be the same that the directory (e.g. my-core)
    instanceDir: the name of the core, it should be the same that the directory (e.g. my-core)
    dataDir: the name of the core, it should be the same that the directory (e.g. my-core)
    config: <empty>
    schema: <empty>