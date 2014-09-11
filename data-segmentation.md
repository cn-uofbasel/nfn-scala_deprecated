
# MetaInformation

Content not fitting into a single content object (> 4kB) needs to be split into segments. To make this segmentation standardized and manageable, we suggest the following approach as well as the ndntlv encoding.

If data is too large to fit into a single content object, it gets segmented into several content objects as required by the CCN protocol. Each segment appends a component to the original content name with the segmentation prefix as well as the segment number (must be continuous and ordered).

/name/video --segmentation--> /name/video/s0; /name/video/s1; ...

The information about the segmentation (number of segments, segment size, ...) as well as additional information (optional mime type, character encoding, ...) about the content is put into an additional content object named with the original content name (here /name/video). This means that data of each content object is encoded in a specific way requiring that the data of a single content object follows this encoding as well. 

For a data stream metainformation about the segmentation needs to be different. For example it is not possible to know the number of segments beforehand. On the other hand information like bitrate about the datastream can be useful. Therefore there is a second metainformation encoding for data streams.

    content ::= CONTENT-TYPE TLV-LENGTH
                    mimeType?
                    charSet?


    mimeType ::= MIME-TYPE TLV-LENGTH BYTE+
    charSet ::= CHAR-SET-TYPE TLV-LENGTH BYTE+

    metadataContent ::= METADATA-CONTENT-TYPE TLV-LENGTH
                            numOfSegments
                            segmentSize
                            lastSegmentSize?
                            segmentName? // default is s, resulting in s0, s1, ...
                            mimeType?
                            charSet?
                            // segmentInfo? could be used to indicate if content contains tlv? 
                            // No size for the full packet because it could be larger than an integer value for TLV. This encoding still still limites max content size in theory, but max packet size is around 2^64*4kB


    numOfSegments  ::= NUM-SEGMENT-TYPE TLV-LENGTH nonNegativeInteger
    segmentSize  ::= SEGMENT-SIZE-TYPE TLV-LENGTH nonNegativeInteger
    lastSegmentSize  ::= LAST-SEGMENT-SIZE-TYPE TLV-LENGTH nonNegativeInteger
    segmentName ::= SEGMENT-NAME-TYPE TLV-LENGTH BYTE+

    metadataStream ::= METADATA-STREAM-TYPE TLV-LENGTH BYTE+



    metadataStream ::= METADATA-STREAM-TYPE TLV-LENGTH
                            segmentSize
                            bitrate?
                            segmentName? // default is s, resulting in s0, s1, ...
                            mimeType?
                            charSet?


    bitrate ::= BITRATE-TYPE TLV-LENGTH nonNegativeInteger



Type values (starting from: 32767)
    CONTENT-TYPE 40000
    METADATA-CONTENT-TYPE 40001
    METADATA-STREAM-TYPE 40002

    // both
    SEGMENT-SIZE-TYPE 40010
    MIME-TYPE 40011
    CHAR-SET-TYPE 40012
    SEGMENT-NAME-TYPE 40013

    // metadataContent
    NUM-SEGMENTS-TYPE 40010
    LAST-SEGMENT-SIZE-TYPE 40011

    // metadataStream
    BITRATE-TYPE TLV-LENGTH 40020


