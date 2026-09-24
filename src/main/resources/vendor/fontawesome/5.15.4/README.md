# Font Awesome Free 5.15.4 (vendored copy)

The PDF templates (`src/main/resources/templates/pdf/**`) use Font Awesome icons. iText used to
fetch the stylesheet and the fonts over the network for every single rendering. They are now read
from here on the classpath, so PDF generation does not depend on an external host at all.

## Why the addresses in the templates were not changed

The templates still reference the original address:

    https://use.fontawesome.com/releases/v5.15.4/css/all.css

`PdfCachingResourceRetriever` maps the prefix `https://use.fontawesome.com/releases/v5.15.4/` to
the directory `vendor/fontawesome/5.15.4/`. Because `all.css` refers to the fonts using relative
addresses (`../webfonts/...`), iText resolves those against the original address, so they match the
same mapping. This meant the templates needed no changes and no `baseUri` workaround, which would
not have worked from inside a JAR.

## Files

These four are exactly the resources iText requests when rendering these templates. Verified by
rendering all five PDF templates with the network blocked: 0 differing pixels and 0 network
requests.

| File | Bytes | SHA-256 |
| --- | --- | --- |
| `css/all.css` | 59305 | `99464ceb71bc9bbdcc72275faefe44f98eb5cbb6b5d8ee665b87b35376f1a96e` |
| `webfonts/fa-brands-400.woff2` | 76736 | `8ea8791754915a898a3100e63e32978a6d1763be6df8e73a39d3a90d691cdeef` |
| `webfonts/fa-regular-400.woff2` | 13224 | `e42a88444448ac3d60549cc7c1ff2c8a9cac721034c073d80a14a44e79730cca` |
| `webfonts/fa-solid-900.woff2` | 78268 | `9834b82ad26e2a37583d22676a12dd2eb0fe7c80356a2114d0db1aa8b3899537` |

`all.css` is the unmodified original file. It also references the eot/woff/ttf/svg formats, but
iText picks woff2, so only those three fonts are needed.

## Updating

    BASE=https://use.fontawesome.com/releases/v5.15.4
    curl -fo css/all.css "$BASE/css/all.css"
    for f in fa-brands-400.woff2 fa-regular-400.woff2 fa-solid-900.woff2; do
        curl -fo "webfonts/$f" "$BASE/webfonts/$f"
    done

If the version changes, update both the addresses in the templates and the `VENDORED_RESOURCES`
mapping in `PdfCachingResourceRetriever`, then run `PdfCachingResourceRetrieverTest`.

Before upgrading to a newer major version, note that Font Awesome 6.2 and later emit glyphs through
CSS custom properties (`--fa: "\f058"; content: var(--fa)`) instead of a literal `content` value.
iText's CSS engine does not support `var()`, so icons would silently disappear. Font Awesome 7 also
drops the version 5 name aliases, which would break `fa-theater-masks`. Verify by rendering before
upgrading.

## Licence

Font Awesome Free, https://fontawesome.com, (c) Fonticons, Inc.

- Icons (SVG/font files): CC BY 4.0
- Fonts: SIL OFL 1.1
- Code (`all.css`): MIT

More information: https://fontawesome.com/license/free

