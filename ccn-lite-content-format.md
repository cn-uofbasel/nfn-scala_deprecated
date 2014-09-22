
# ccn-lite content format in NDNTLV (draft 0.1)

The ccn-lite protocol does only allow content objects with max 4kB of data. Larger data need to be split into several segments. To make this process more manageable and standardized for ccn-lite applications, we suggest the following format in NDNTLV.

If data is too large to fit into a single content object, it gets segmented into several content objects. Each segment appends a component to the original content name with the segmentation prefix as well as the segment number (continuous and ordered starting from 0).

    /name/video --segmentation--> /name/video/s0; /name/video/s1; ...

The information about the segmentation (number of segments, segment size, ...) as well as additional information (optional mime type, character encoding, ...) about the content is put into an additional content object, the metadata object, named with the original content name (here /name/video). Because each content object has to be parsed for this format, we take this opportunity to add additional metainformation to content in a single content object as well (with an additional data field).

Above we proposed that segmentation information is added to a segmented content object. However, this only works if all future content is known. Data streams cannot be implemented this way. We added another explicit content type representing data streams. For a content streams metainformation about the segmentation needs to be different. For example it is not possible to know the number of segments beforehand. On the other hand information like bitrate about the datastream or possibly how much time a single segment represnts could be useful.

The details of this encoding is only the first draft, several topics are open for discussion:
- What additional meta information is usedufl? (we have mimetype and charset)
- Should data be optional in a singleSegmentContent? This would enable to fetch only metainformation of a single content object
- What are some useful meta informations for a content object stream?
- Segments them selves need to be encoded as singleSegmentContent. Is there any benefit by having additional information for each segment?
- Should there be a field for an application specific custom tlv?

```
singleSegmentContent ::= SINGLE-SEGMENT-CONTENT-TYPE TLV-LENGTH
                mimeType?
                charSet?
                data

mimeType ::= MIME-TYPE TLV-LENGTH BYTE+
charSet ::= CHAR-SET-TYPE TLV-LENGTH BYTE+
data ::= CHAR-SET-TYPE TLV-LENGTH BYTE+


multiSegmentContent ::= MULTI-SEGMENT-CONTENT-TYPE TLV-LENGTH
                            mimeType?
                            charSet?
                            segmentName? // default is s, resulting in s0, s1, ...
                            numOfSegments
                            segmentSize
                            lastSegmentSize

segmentName ::= SEGMENT-NAME-TYPE TLV-LENGTH BYTE+
numOfSegments  ::= NUM-SEGMENT-TYPE TLV-LENGTH nonNegativeInteger
segmentSize  ::= SEGMENT-SIZE-TYPE TLV-LENGTH nonNegativeInteger
lastSegmentSize  ::= LAST-SEGMENT-SIZE-TYPE TLV-LENGTH nonNegativeInteger


metadataStream ::= STREAM-CONTENT-TYPE TLV-LENGTH
                        mimeType?
                        charSet?
                        segmentName? // default is s, resulting in s0, s1, ...
                        segmentSize
                        bitrate?

bitrate ::= BITRATE-TYPE TLV-LENGTH nonNegativeInteger
```


Type values (applicaion specific values start from: 32767):

```
SINGLE-SEGMENT-CONTENT-TYPE 40000
MULTI-SEGMENT-CONTENT-TYPE 40001
STREAM-CONTENT-TYPE 40002


// general content info
MIME-TYPE 40010
CHAR-SET-TYPE 40011
DATA-TYPE 40012

// metadataMultiSegment
SEGMENT-NAME-TYPE 40020
NUM-SEGMENTS-TYPE 40021
SEGMENT-SIZE-TYPE 40022
LAST-SEGMENT-SIZE-TYPE 40023

// metadataStream
BITRATE-TYPE TLV-LENGTH 40030
```


