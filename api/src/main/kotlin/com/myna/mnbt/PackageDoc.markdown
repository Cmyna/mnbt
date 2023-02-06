# Package com.myna.mnbt

## Data Format Specification

### Nbt Path

A String Format that declares an absolute/relate tag path to an Nbt Tag tree 
structure. The String format use URL like format, the absolute path starts 
with scheme name `mnbt://`, and relate path starts with sub-string `./`

the path actual value is represented like `<tag name1>/<tag name2>/<tag name3>`, 
each tag name/ path segmentation is separated by char '/'. The tag name/ path segment
support any symbol except char '/', if want to use it in tag name, use "\/" instead.

    absolute path sample: mnbt://tag1/tag2/tag3
    relate path sample: ./tag1/tag2/tag3

To represent a list index in a path, use `#<index>` where `<index>` is an integer.

    example: ./tag1/list tag/#5/tag4
