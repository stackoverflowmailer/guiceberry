package junit3_tdtc.tutorial_1_server;

import com.google.common.testing.junit3.TearDownTestCase;
import com.google.guiceberry.controllable.InjectionController;
import com.google.guiceberry.junit3.AutoTearDownGuiceBerry;
import com.google.inject.Inject;

import tutorial_1_server.prod.Pet;
import tutorial_1_server.prod.Featured;
import tutorial_1_server.testing.PetStoreEnv4InjectionControlled;
import tutorial_1_server.testing.WelcomeTestPage;

public class Example4InjectionControlledTest extends TearDownTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    AutoTearDownGuiceBerry.setUp(this, PetStoreEnv4InjectionControlled.class);
  }
  
  @Inject
  WelcomeTestPage welcomeTestPage;
  
  @Inject
  @Featured
  private InjectionController<Pet> featuredPetInjectionController;

  public void testDogAsPotm() {
    Pet expected = Pet.DOG;
    featuredPetInjectionController.setOverride(expected);
    welcomeTestPage.goTo();
    welcomeTestPage.assertFeaturedPetIs(expected);
  }

  public void testCatAsPotm() {
    Pet expected = Pet.CAT;
    featuredPetInjectionController.setOverride(expected);
    welcomeTestPage.goTo();
    welcomeTestPage.assertFeaturedPetIs(expected);
  }
}