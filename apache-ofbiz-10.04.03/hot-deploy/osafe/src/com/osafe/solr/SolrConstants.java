package com.osafe.solr;

import org.apache.solr.common.params.FacetParams;

public class SolrConstants {

    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final String EXTRACT_PRODUCT_FEATURE_PREFIX = "productFeature_";

    public static final String TYPE_PRICE = "price";
    public static final String TYPE_CUSTOMER_RATING = "customerRating";
    public static final String TYPE_PRODUCT_CATEGORY = "productCategoryId";
    public static final String TYPE_TOP_MOST_PRODUCT_CATEGORY = "topMostProductCategoryId";

    public static final String DISPLAY_PRICE_NAME_KEY = "Price";
    public static final String DISPLAY_CUSTOMER_RATING_NAME_KEY = "Customer Rating";

    public static final String ROW_TYPE_PRODUCT = "product";
    public static final String ROW_TYPE_PRODUCT_CATEGORY = "productCategory";
    public static final String ROW_TYPE_FACET_GROUP = "facetGroup";

    public static final String FACET_SORT_DB_SEQ =  "dbseq";
    public static final String FACET_SORT_INDEX =  FacetParams.FACET_SORT_INDEX;
    public static final String FACET_SORT_COUNT =  FacetParams.FACET_SORT_COUNT;

    public static final String SCHEMA_PRODUCT_FEATURE_NAME_ATTR = "name";
    public static final String SCHEMA_PRODUCT_FEATURE_NAME_VALUE_PREFIX = "productFeature_";
    public static final String SCHEMA_PRODUCT_FEATURE_INDEXED_ATTR = "indexed";
    public static final String SCHEMA_PRODUCT_FEATURE_INDEXED_VALUE = "true";
    public static final String SCHEMA_PRODUCT_FEATURE_MULTIVALUED_ATTR = "multivalued";
    public static final String SCHEMA_PRODUCT_FEATURE_MULTIVALUED_VALUE = "true";
    public static final String SCHEMA_PRODUCT_FEATURE_OMITNORMS_ATTR = "omitnorms";
    public static final String SCHEMA_PRODUCT_FEATURE_OMITNORMS_VALUE = "true";
    public static final String SCHEMA_PRODUCT_FEATURE_REQUIRED_ATTR = "required";
    public static final String SCHEMA_PRODUCT_FEATURE_REQUIRED_VALUE = "false";
    public static final String SCHEMA_PRODUCT_FEATURE_STORED_ATTR = "stored";
    public static final String SCHEMA_PRODUCT_FEATURE_STORED_VALUE = "true";
    public static final String SCHEMA_PRODUCT_FEATURE_TYPE_ATTR = "type";
    public static final String SCHEMA_PRODUCT_FEATURE_TYPE_VALUE = "text_ws";

    public static final String SCHEMA_PRODUCT_FEATURE_SOURCE_ATTR = "source";
    public static final String SCHEMA_PRODUCT_FEATURE_SOURCE_VALUE_PREFIX = "productFeature_";
    public static final String SCHEMA_PRODUCT_FEATURE_DEST_ATTR = "dest";
    public static final String SCHEMA_PRODUCT_FEATURE_DEST_VALUE = "searchText";

}
