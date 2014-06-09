% Linguistic Data Validation and Observation
% John P. M<sup>c</sup>Crae
% 12th June 2014

# Problem

<div>
  <img src="icon_47466.png" width="10%"/>
  Lots of resources
</div>
<div>
  <img src="icon_44875.png" width="10%"/>
  Frequently contain flaws
</div>

# Case Study: WordNets

Sources:

# Case Study: WordNets

Formats:

# Case Study: WordNets

Data quality:

# Solution

<div>
  <img src="icon_40065.png" width="10%"/>
  Data certification
</div>
<div>
  <img src="icon_3683.png" width="10%"/>
  Metadata
</div>

# Data certification

Requirements:

* Open data
* Single URL
* Standard format

# Zrtifi

[click](design.html)

# Backend

<img src="Zrtifi Backend Flow.png"/>

# Rules

* We don't store data
* New steps can be added via GitHub
* Linear, stream-based validation

# Data-sniffer

Inversion of control service

    class Sniffer {
       boolean isInFormat(String fileName, 
                          byte[] firstKilobyte);
       String chain();
    }

# Validators

* Shell scripts
* Return JSON-LD

<pre>
{
    "@context": "http://...",
    "result": "success",
    "void:triples": 123456,
    "next" : "rdfunit"
}
</pre>

# Observatory

* Check dataset every 24 hours
* HTTP handshake (200 Accept)
* Check `Last-Modified`

# Metadata

<div class="leftcol">
  <div>
     Datahub.io
  </div>
  <div>
     Metashare
  </div>
</div>
<div class="rightcol">
  <div>
    CLARIN
  </div>
  <div>
    LRE Map
  </div>
</div>

# Statistics

# LingHub

* Define common metadata (DCAT + VoID)
* Bring everything 'under one roof'
* Find duplicates

# Zrtifi-LingHub
   
* Incorporate output from Zrtifi
* Triple counts
* Validation

# Better LLOD Cloud 


# Image credits

Heavy Load designed by Juan Pablo Bravo from the Noun Project

Floppy Disk designed by Julien Deveaux from the Noun Project

Warranty designed by Eugen Belyakoff from the Noun Project

Network designed by Ben Rex Furneaux from the Noun Project
