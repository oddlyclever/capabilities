@file:JsModule("h3-js")

package com.oddlyclever.h3spatial.h3js

// The h3-js functions H3Native calls, declared from h3-js's own typings (dist/types.d.ts) in the types
// Kotlin/Wasm can pass to JavaScript. Indexes cross as canonical H3 strings and coordinates as
// [lat, lng] pairs in degrees. Kept in their own package so they do not shadow H3Native's members.

internal external fun latLngToCell(lat: Double, lng: Double, res: Int): String
internal external fun cellToLatLng(h3Index: String): JsArray<JsNumber>
internal external fun cellToBoundary(h3Index: String): JsArray<JsArray<JsNumber>>
internal external fun isValidCell(h3Index: String): Boolean
internal external fun isPentagon(h3Index: String): Boolean
internal external fun gridDisk(h3Index: String, ringSize: Int): JsArray<JsString>
internal external fun gridDistance(origin: String, destination: String): Double
internal external fun areNeighborCells(origin: String, destination: String): Boolean
internal external fun cellToParent(h3Index: String, res: Int): String
internal external fun cellToChildren(h3Index: String, res: Int): JsArray<JsString>
