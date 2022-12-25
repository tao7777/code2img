# Code2Img

Code2Img is a tree-based code clone detector, which satisfies scalability while detecting complicated clones effectively.
Given the source code, we first perform clone filtering by the inverted index to locate the suspected clones.
For suspected clones, we create the adjacency images based on the adjacency matrix of the normalized abstract syntax tree (AST).
Then we design the image encoder to further highlight the structural details and remove the noise within the image.
Specifically, we employ the Markov model to encode the adjacency images into state probability images and remove the useless pixels of the images.
By this, the original complex tree can be transformed into a one-dimensional vector while preserving the structural information of the AST.
Finally, we detect clones by calculating the Jaccard Similarity of these vectors.

The framework of Code2Img consists of four main phases:
Data Preprocessing, Clone Filtering, Image Transforming, and Clone Detecting.

1. Data Preprocessing: This phase aims to normalize the type-specific tokens of the AST and generate the inverted index by N-line hash for each code block of the normalized code.
2. Clone Filtering: This phase is targeted to search for clone candidates in the inverted index and calculate the N-lines similarity to obtain the suspected clones.
3. Image Transforming: For the suspected clones, this phase first transforms the AST into an adjacency image by the adjacency matrix of AST's node types.
    Then encode the adjacency image with the Markov model to obtain the state probability image and remove its useless pixels to get the final vector.
4. Clone Detecting: This phase aims to detect clones within the suspected clones by calculating the Jaccard similarity between their final vectors.
