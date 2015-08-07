package com.github.onsdigital.zebedee.content.page.taxonomy;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.page.taxonomy.base.TaxonomyNode;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 *
 * Represents a product node that holds links to related statistics under the product ( e.g. cpi )
 */
public class TaxonomyProductNode extends TaxonomyNode {

    private List<Link> items;
    private List<Link> datasets;
    private List<Link> statsBulletins;
    private List<Link> relatedArticles;

    @Override
    public PageType getType() {
        return PageType.product_page;
    }

    public List<Link> getItems() {
        return items;
    }

    public void setItems(List<Link> items) {
        this.items = items;
    }

    public List<Link> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Link> datasets) {
        this.datasets = datasets;
    }

    public List<Link> getStatsBulletins() {
        return statsBulletins;
    }

    public void setStatsBulletins(List<Link> statsBulletins) {
        this.statsBulletins = statsBulletins;
    }


    public List<Link> getRelatedArticles() {
        return relatedArticles;
    }

    public void setRelatedArticles(List<Link> relatedArticles) {
        this.relatedArticles = relatedArticles;
    }
}
