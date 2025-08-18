#ifndef TREE_SITTER_API_H_
#define TREE_SITTER_API_H_

#ifdef __cplusplus
extern "C" {
#endif

#include <stdbool.h>
#include <stdint.h>
#include <stdlib.h>

/******************************************************************************
 * Section - TSLanguage
 ******************************************************************************/

/**
 * An opaque object that defines how to parse a particular language. The
 * language should be retrieved by calling a language-specific function (e.g.
 * `tree_sitter_json`), not by using this struct directly.
 */
typedef struct TSLanguage TSLanguage;

/******************************************************************************
 * Section - TSParser
 ******************************************************************************/

/**
 * An opaque object that is used to produce a `TSTree` based on some source
 * code.
 */
typedef struct TSParser TSParser;

/**
 * Create a new parser.
 */
TSParser *ts_parser_new(void);

/**
 * Delete the parser, freeing all of the memory that it used.
 */
void ts_parser_delete(TSParser *parser);

/**
 * Set the language that the parser should use for parsing.
 *
 * Returns a boolean indicating whether or not the language was successfully
 * assigned. True means assignment succeeded. False means there was a version
 * mismatch: the language was generated with an incompatible version of the
 * Tree-sitter CLI. Check the language's version using `ts_language_version`
 * and compare it to `TREE_SITTER_LANGUAGE_VERSION` and
 * `TREE_SITTER_MIN_COMPATIBLE_LANGUAGE_VERSION`.
 */
bool ts_parser_set_language(TSParser *self, const TSLanguage *language);

/******************************************************************************
 * Section - TSTree
 ******************************************************************************/

/**
 * An opaque object that represents a single parse tree.
 */
typedef struct TSTree TSTree;

/**
 * Parse a string.
 */
TSTree *ts_parser_parse_string(
  TSParser *self,
  const TSTree *old_tree,
  const char *string,
  uint32_t length
);

/**
 * Delete the syntax tree, freeing all of the memory that it used.
 */
void ts_tree_delete(TSTree *self);

/**
 * Get the root node of the syntax tree.
 */
typedef struct TSNode {
  uint32_t context[4];
  const void *id;
  const TSTree *tree;
} TSNode;

TSNode ts_tree_root_node(const TSTree *self);

/******************************************************************************
 * Section - TSNode
 ******************************************************************************/

/**
 * A single node in a syntax tree.
 */
typedef struct {
  uint32_t row;
  uint32_t column;
} TSPoint;

/**
 * Get the node's type as a null-terminated string.
 */
const char *ts_node_type(TSNode self);

/**
 * Get the node's start position in terms of rows and columns.
 */
TSPoint ts_node_start_point(TSNode self);

/**
 * Get the node's end position in terms of rows and columns.
 */
TSPoint ts_node_end_point(TSNode self);

/**
 * Get the node's start byte offset.
 */
uint32_t ts_node_start_byte(TSNode self);

/**
 * Get the node's end byte offset.
 */
uint32_t ts_node_end_byte(TSNode self);

/**
 * Get the number of children for the given node.
 */
uint32_t ts_node_child_count(TSNode self);

/**
 * Get the node's child at the given index, where zero represents the first
 * child.
 */
TSNode ts_node_child(TSNode self, uint32_t child_index);

/**
 * Check if the node is null.
 */
bool ts_node_is_null(TSNode self);

#ifdef __cplusplus
}
#endif

#endif  // TREE_SITTER_API_H_