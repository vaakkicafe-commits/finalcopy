describe('Lee Vaakki™ WordPress Integration Test', () => {
  const domains = [
    'https://leevaakkicafe.com',
    'https://leevaakkidhaba.com',
    'https://leevaakkipvtld.com'
  ];

  domains.forEach((domain) => {
    it(`should successfully navigate the checkout flow on ${domain}`, () => {
      // 1. Visit the store
      cy.visit(domain);
      cy.get('body').should('be.visible');

      // 2. Navigate to Menu/Shop
      cy.contains('Menu').click({ force: true });

      // 3. Add a sample item to cart
      // We look for any "Add to cart" button
      cy.get('.add_to_cart_button').first().click();

      // 4. View Cart
      cy.contains('View cart').click({ force: true });
      cy.url().should('include', '/cart');

      // 5. Proceed to Checkout
      cy.contains('Proceed to checkout').click();
      cy.url().should('include', '/checkout');

      // 6. Verify Checkout Form is present
      cy.get('#billing_first_name').should('exist');
      cy.get('#billing_email').should('exist');

      // 7. Check for Lee Vaakki™ Branding
      cy.contains('Lee Vaakki™').should('exist');
    });
  });

  it('should verify the .well-known/assetlinks.json exists for deep linking', () => {
    domains.forEach((domain) => {
      cy.request(`${domain}/.well-known/assetlinks.json`).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.body[0].target.package_name).to.eq('com.leevaakki.cafe');
      });
    });
  });
});
